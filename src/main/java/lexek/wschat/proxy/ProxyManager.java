package lexek.wschat.proxy;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lexek.wschat.chat.*;
import lexek.wschat.db.dao.ProxyDao;
import lexek.wschat.db.jooq.tables.pojos.ChatProxy;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.e.EntityNotFoundException;
import lexek.wschat.db.model.e.InvalidDataException;
import lexek.wschat.services.AbstractService;
import lexek.wschat.services.JournalService;
import lexek.wschat.services.MessageConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProxyManager extends AbstractService implements MessageConsumerService {
    private final Logger logger = LoggerFactory.getLogger(ProxyManager.class);
    private final Map<String, ProxyProvider> providers = new HashMap<>();
    private final Multimap<Long, Proxy> proxies = HashMultimap.create();
    private final ProxyDao proxyDao;
    private final RoomManager roomManager;
    private final JournalService journalService;

    public ProxyManager(ProxyDao proxyDao, RoomManager roomManager, JournalService journalService) {
        super("proxyManager");
        this.proxyDao = proxyDao;
        this.roomManager = roomManager;
        this.journalService = journalService;
    }

    public void registerProvider(ProxyProvider proxyProvider) {
        providers.put(proxyProvider.getName(), proxyProvider);
    }

    public Proxy newProxy(UserDto admin, Room room, String providerName, String remoteRoom, String name, String token, boolean outbound) {
        ProxyProvider provider = providers.get(providerName);
        if (token != null && name != null && provider.isSupportsAuthentication() && !provider.validateCredentials(name, token)) {
            throw new InvalidDataException("Invalid credentials.");
        }
        ChatProxy chatProxy = new ChatProxy(null, room.getId(), providerName, name, token, remoteRoom, outbound);
        proxyDao.store(chatProxy);
        journalService.newProxy(admin, room, providerName, remoteRoom);
        if (provider != null) {
            Proxy proxy = provider.newProxy(chatProxy.getId(), room, remoteRoom, name, token, outbound);
            proxies.put(room.getId(), proxy);
            proxy.start();
            return proxy;
        } else {
            throw new InvalidDataException("Unknown proxy name");
        }
    }

    public void remove(UserDto admin, Room room, String provider, String remoteRoom) {
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
            if (proxy.state() == ProxyState.RUNNING) {
                proxy.stop();
            } else {
                throw new InvalidDataException("You can stop only running proxy.");
            }
        } else {
            throw new EntityNotFoundException("Proxy doesn't exist.");
        }
    }

    public void startProxy(Room room, String provider, String remoteRoom) {
        Proxy proxy = getProxy(room, provider, remoteRoom);
        if (proxy != null) {
            if (proxy.state() == ProxyState.STOPPED || proxy.state() == ProxyState.FAILED) {
                proxy.start();
            } else {
                throw new InvalidDataException("You can start only stopped or failed proxy.");
            }
        } else {
            throw new EntityNotFoundException("Proxy doesn't exist.");
        }
    }

    @Override
    public void consume(Connection connection, Message message, BroadcastFilter filter) {
        if (filter.getType() == BroadcastFilter.Type.ROOM && message.getType() == MessageType.MSG) {
            Room room = (Room) filter.getData();
            proxies.get(room.getId())
                .stream()
                .filter(proxy -> proxy.state() == ProxyState.RUNNING)
                .filter(Proxy::outboundEnabled)
                .forEach(proxy -> proxy.onMessage(connection, message));
        }
    }

    public void moderate(Room room, String providerName, String remoteRoom, ModerationOperation operation, String name) {
        Proxy proxy = getProxy(room, providerName, remoteRoom);
        if (proxy != null && proxy.state() == ProxyState.RUNNING) {
            if (proxy.provider().supports(operation)) {
                proxy.moderate(operation, name);
            }
        }
    }

    @Override
    protected void start0() {
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
                chatProxy.getAuthName(),
                chatProxy.getAuthKey(),
                chatProxy.getEnableOutbound()
            );
            proxies.put(room.getId(), proxy);
            proxy.start();
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
}
