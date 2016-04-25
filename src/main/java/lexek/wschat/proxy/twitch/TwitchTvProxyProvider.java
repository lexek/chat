package lexek.wschat.proxy.twitch;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.NotificationService;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class TwitchTvProxyProvider extends ProxyProvider {
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private final AuthenticationManager authenticationManager;
    private final EventLoopGroup eventLoopGroup;
    private final ProxyAuthService authService;
    private final NotificationService notificationService;

    public TwitchTvProxyProvider(
        AtomicLong messageId,
        MessageBroadcaster messageBroadcaster,
        AuthenticationManager authenticationManager,
        EventLoopGroup eventLoopGroup,
        ProxyAuthService authService,
        NotificationService notificationService
    ) {
        super("twitch", true, true, false, ImmutableSet.of("twitch"), EnumSet.allOf(ModerationOperation.class));
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.authenticationManager = authenticationManager;
        this.eventLoopGroup = eventLoopGroup;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, Long proxyAuthId, boolean outbound) {
        return new TwitchTvChatProxy(
            notificationService, id, this, room, remoteRoom, proxyAuthId, outbound, messageId, messageBroadcaster,
            authenticationManager, eventLoopGroup, authService
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        return true;
    }
}
