package lexek.wschat.proxy.twitch;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.*;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.JsonResponseHandler;
import lexek.wschat.util.JsonStreamResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class TwitchTvProxyProvider extends ProxyProvider {
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private final TwitchCredentialsService credentialsService;
    private final EventLoopGroup eventLoopGroup;
    private final ProxyAuthService authService;
    private final NotificationService notificationService;

    @Inject
    public TwitchTvProxyProvider(
        @Named("messageId") AtomicLong messageId,
        MessageBroadcaster messageBroadcaster,
        TwitchCredentialsService credentialsService,
        @Named("proxyEventLoopGroup") EventLoopGroup eventLoopGroup,
        ProxyAuthService authService,
        NotificationService notificationService
    ) {
        super("twitch", true, true, false, false, ImmutableSet.of("twitch"), EnumSet.allOf(ModerationOperation.class));
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.credentialsService = credentialsService;
        this.eventLoopGroup = eventLoopGroup;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, Long proxyAuthId, boolean outbound) {
        return new TwitchTvChatProxy(
            notificationService, id, this, room, remoteRoom, proxyAuthId, outbound, messageId, messageBroadcaster,
            credentialsService, eventLoopGroup, authService
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        return true;
    }
}
