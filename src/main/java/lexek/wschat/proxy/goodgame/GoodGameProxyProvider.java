package lexek.wschat.proxy.goodgame;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.services.NotificationService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GoodGameProxyProvider extends ProxyProvider {
    private final NotificationService notificationService;
    private final ProxyAuthService proxyAuthService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messsageId;
    private final GoodGameApiClient apiClient;

    @Inject
    public GoodGameProxyProvider(
        NotificationService notificationService,
        ProxyAuthService proxyAuthService,
        @Named("proxyEventLoopGroup") EventLoopGroup eventLoopGroup,
        MessageBroadcaster messageBroadcaster,
        @Named("messageId") AtomicLong messsageId,
        GoodGameApiClient apiClient
    ) {
        super("goodgame", true, false, false, false, ImmutableSet.of("goodgame"), EnumSet.noneOf(ModerationOperation.class));
        this.notificationService = notificationService;
        this.proxyAuthService = proxyAuthService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messsageId = messsageId;
        this.apiClient = apiClient;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, Long userAuthId, boolean outbound) {
        String userId = null;
        if (userAuthId != null) {
            SocialProfile profile = proxyAuthService.getProfile(userAuthId);
            if (profile != null) {
                userId = profile.getId();
            }
        }
        return new GoodGameChatProxy(
            notificationService,
            messageBroadcaster,
            eventLoopGroup,
            messsageId,
            this,
            id,
            room,
            remoteRoom,
            userId,
            apiClient,
            userAuthId
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        try {
            return apiClient.getChannelId(remoteRoom) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
