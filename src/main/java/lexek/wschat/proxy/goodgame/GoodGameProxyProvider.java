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
import org.apache.http.client.HttpClient;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class GoodGameProxyProvider extends ProxyProvider {
    private final NotificationService notificationService;
    private final ProxyAuthService proxyAuthService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messsageId;
    private final HttpClient httpClient;

    public GoodGameProxyProvider(
        NotificationService notificationService,
        ProxyAuthService proxyAuthService, EventLoopGroup eventLoopGroup,
        MessageBroadcaster messageBroadcaster,
        AtomicLong messsageId,
        HttpClient httpClient
    ) {
        super("goodgame", true, false, false, ImmutableSet.of("goodgame"), EnumSet.noneOf(ModerationOperation.class));
        this.notificationService = notificationService;
        this.proxyAuthService = proxyAuthService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messsageId = messsageId;
        this.httpClient = httpClient;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, Long userAuthId, boolean outbound) {
        CredentialsProvider credentialsProvider = null;
        String userId = null;
        if (userAuthId != null) {
            credentialsProvider = new CredentialsProvider(httpClient, proxyAuthService, userAuthId);
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
            credentialsProvider
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        return true;
    }
}
