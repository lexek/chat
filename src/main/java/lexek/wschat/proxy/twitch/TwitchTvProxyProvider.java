package lexek.wschat.proxy.twitch;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.proxy.*;
import lexek.wschat.services.NotificationService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TwitchTvProxyProvider extends ProxyProvider {
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private final TwitchCredentialsService credentialsService;
    private final EventLoopGroup eventLoopGroup;
    private final ProxyAuthService authService;
    private final NotificationService notificationService;
    private final CheermotesProvider cheermotesProvider;

    @Inject
    public TwitchTvProxyProvider(
        @Named("messageId") AtomicLong messageId,
        MessageBroadcaster messageBroadcaster,
        TwitchCredentialsService credentialsService,
        @Named("proxyEventLoopGroup") EventLoopGroup eventLoopGroup,
        ProxyAuthService authService,
        NotificationService notificationService,
        CheermotesProvider cheermotesProvider
    ) {
        super("twitch", true, true, false, false, ImmutableSet.of("twitch"), EnumSet.allOf(ModerationOperation.class));
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.credentialsService = credentialsService;
        this.eventLoopGroup = eventLoopGroup;
        this.authService = authService;
        this.notificationService = notificationService;
        this.cheermotesProvider = cheermotesProvider;
    }

    @Override
    public Proxy newProxy(ProxyDescriptor descriptor) {
        return new TwitchTvChatProxy(
            descriptor, notificationService, messageId, messageBroadcaster, credentialsService, eventLoopGroup, authService, cheermotesProvider
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom, Long authId) {
        return true;
    }
}
