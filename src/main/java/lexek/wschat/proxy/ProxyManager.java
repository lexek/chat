package lexek.wschat.proxy;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.chat.e.BadRequestException;
import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.dao.ProxyDao;
import lexek.wschat.db.jooq.tables.pojos.ChatProxy;
import lexek.wschat.db.model.ProxyAuth;
import lexek.wschat.db.tx.Transactional;
import lexek.wschat.services.JournalService;
import lexek.wschat.services.MessageConsumerService;
import lexek.wschat.services.managed.AbstractManagedService;
import lexek.wschat.services.managed.InitStage;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProxyManager extends AbstractManagedService implements MessageConsumerService {
    private final Logger logger = LoggerFactory.getLogger(ProxyManager.class);
    private final Map<String, ProxyProvider> providers = new HashMap<>();
    private final Multimap<Long, Proxy> proxies = HashMultimap.create();
    private final ProxyDao proxyDao;
    private final RoomManager roomManager;
    private final JournalService journalService;
    private final ProxyAuthService proxyAuthService;

    @Inject
    public ProxyManager(ProxyDao proxyDao, RoomManager roomManager, JournalService journalService, ProxyAuthService proxyAuthService) {
        super("proxyManager", InitStage.ASYNC_SERVICE);
        this.proxyDao = proxyDao;
        this.roomManager = roomManager;
        this.journalService = journalService;
        this.proxyAuthService = proxyAuthService;
    }

    @Inject
    public void init(Iterable<ProxyProvider> proxyProviders) {
        proxyProviders.forEach(this::registerProvider);
    }

    public void registerProvider(ProxyProvider proxyProvider) {
        providers.put(proxyProvider.getName(), proxyProvider);
    }

    @Transactional
    public Proxy newProxy(
        User admin,
        Room room,
        String providerName,
        String remoteRoom,
        ProxyAuth proxyAuth,
        boolean outbound
    ) {
        ProxyProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new BadRequestException("Unknown proxy name");
        }
        if (proxyAuth != null && !provider.supportsAuthService(proxyAuth.getService())) {
            throw new BadRequestException("Invalid credentials");
        }
        if (!provider.validateRemoteRoom(remoteRoom)) {
            throw new BadRequestException("Invalid remote room");
        }
        if (exists(room, providerName, remoteRoom)) {
            throw new BadRequestException("Proxy with same provider and remote room already exists");
        }
        Long proxyAuthId = proxyAuth != null ? proxyAuth.getId() : null;
        ChatProxy chatProxy = new ChatProxy(null, room.getId(), providerName, remoteRoom, proxyAuthId, outbound);
        proxyDao.store(chatProxy);
        journalService.newProxy(admin, room, providerName, remoteRoom);
        Proxy proxy = provider.newProxy(chatProxy.getId(), room, remoteRoom, proxyAuthId, outbound);
        proxies.put(room.getId(), proxy);
        proxy.start();
        return proxy;
    }

    private boolean exists(Room room, String providerName, String remoteRoom) {
        return proxies
            .get(room.getId())
            .stream()
            .anyMatch(e -> e.provider().getName().equals(providerName) && e.remoteRoom().equals(remoteRoom));
    }

    @Transactional
    public void remove(User admin, Room room, String provider, String remoteRoom) {
        Proxy proxy = getProxy(room, provider, remoteRoom);
        if (proxy != null) {
            proxies.remove(room.getId(), proxy);
            proxy.stop();
            proxyDao.remove(proxy.id());
            journalService.deletedProxy(admin, room, provider, proxy.remoteRoom());
        } else {
            throw new EntityNotFoundException("Proxy doesn't exist.");
        }
    }

    public void stopProxy(Room room, String provider, String remoteRoom) {
        Proxy proxy = getProxy(room, provider, remoteRoom);
        if (proxy != null) {
            if (proxy.state() == ProxyState.RUNNING || proxy.state() == ProxyState.RECONNECTING) {
                proxy.stop();
            } else {
                throw new BadRequestException("You can stop only running proxy.");
            }
        } else {
            throw new EntityNotFoundException("Proxy doesn't exist.");
        }
    }

    public void startProxy(Room room, String provider, String remoteRoom) {
        Proxy proxy = getProxy(room, provider, remoteRoom);
        if (proxy != null) {
            if (proxy.state() == ProxyState.STOPPED) {
                proxy.start();
            } else {
                throw new BadRequestException("You can start only stopped or failed proxy.");
            }
        } else {
            throw new EntityNotFoundException("Proxy doesn't exist.");
        }
    }

    @Override
    public void consume(Message message, BroadcastFilter filter) {
        if (filter.getType() == BroadcastFilter.Type.ROOM && message.getType() == MessageType.MSG) {
            Room room = (Room) filter.getData();
            proxies.get(room.getId())
                .stream()
                .filter(proxy -> proxy.state() == ProxyState.RUNNING)
                .filter(Proxy::outboundEnabled)
                .forEach(proxy -> proxy.onMessage(message));
        }
    }

    public void moderate(Room room, String providerName, String remoteRoom, ModerationOperation operation, String name) {
        Proxy proxy = getProxy(room, providerName, remoteRoom);
        if (proxy != null && proxy.state() == ProxyState.RUNNING) {
            if (proxy.provider().supportsModerationOperation(operation)) {
                proxy.moderate(operation, name);
            }
        }
    }

    @Override
    public void start() {
        proxyAuthService.loadTokens();
        for (ChatProxy chatProxy : proxyDao.getAll()) {
            ProxyProvider provider = providers.get(chatProxy.getProviderName());
            Room room = roomManager.getRoomInstance(chatProxy.getRoomId());
            if (room == null) {
                logger.warn("Room not found for proxy {}", chatProxy.getId());
                continue;
            }
            if (provider == null) {
                logger.warn("Provider not found for proxy {} ({})", chatProxy.getId(), chatProxy.getProviderName());
                continue;
            }
            Proxy proxy = provider.newProxy(
                chatProxy.getId(),
                room,
                chatProxy.getRemoteRoom(),
                chatProxy.getAuthId(),
                chatProxy.getEnableOutbound()
            );
            proxies.put(room.getId(), proxy);
            try {
                proxy.start();
            } catch (Exception e) {
                logger.error("unable to start proxy {}", chatProxy.getId(), e);
            }
        }
    }

    @Override
    public void stop() {
        proxies.values().forEach(lexek.wschat.proxy.Proxy::stop);
    }

    @Override
    public HealthCheck getHealthCheck() {
        return new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy();
            }
        };
    }

    private Proxy getProxy(Room room, String provider, String remoteRoom) {
        Collection<Proxy> roomProxies = proxies.get(room.getId());
        if (roomProxies != null) {
            for (Proxy e : roomProxies) {
                if (e.provider().getName().equals(provider) && e.remoteRoom().equals(remoteRoom)) {
                    return e;
                }
            }
        }
        return null;
    }

    public Collection<Proxy> getProxiesByRoom(Room room) {
        return proxies.get(room.getId());
    }

    public Collection<ProxyProvider> getProviders() {
        return providers.values();
    }

    public ProxyProvider getProvider(String provider) {
        return providers.get(provider);
    }
}
