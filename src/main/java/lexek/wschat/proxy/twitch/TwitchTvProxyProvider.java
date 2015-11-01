package lexek.wschat.proxy.twitch;

import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.social.SocialAuthProfile;
import lexek.wschat.security.social.TwitchTvSocialAuthService;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class TwitchTvProxyProvider extends ProxyProvider {
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private final AuthenticationManager authenticationManager;
    private final EventLoopGroup eventLoopGroup;
    private final TwitchTvSocialAuthService authService;

    public TwitchTvProxyProvider(AtomicLong messageId,
                                 MessageBroadcaster messageBroadcaster,
                                 AuthenticationManager authenticationManager,
                                 EventLoopGroup eventLoopGroup, TwitchTvSocialAuthService authService) {
        super("twitch", true, true, EnumSet.allOf(ModerationOperation.class));
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.authenticationManager = authenticationManager;
        this.eventLoopGroup = eventLoopGroup;
        this.authService = authService;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, String name, String token, boolean outbound) {
        return new TwitchTvChatProxy(
            id, this, room, remoteRoom, name, token, outbound,
            messageId, messageBroadcaster, authenticationManager, eventLoopGroup
        );
    }

    @Override
    public boolean validateCredentials(String name, String token) {
        try {
            Set<String> scopes = authService.getScopes(token);
            SocialAuthProfile profile = authService.getProfile(token);
            return scopes.contains("chat_login") && profile.getName().equals(name);
        } catch (IOException e) {
            throw new InternalErrorException(e);
        }
    }
}
