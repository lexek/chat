package lexek.wschat.proxy.twitter;

import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.services.NotificationService;

import javax.net.ssl.SSLException;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class TwitterProxyProvider extends ProxyProvider {
    private final NotificationService notificationService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final TwitterCredentials credentials;

    public TwitterProxyProvider(
        NotificationService notificationService,
        MessageBroadcaster messageBroadcaster,
        EventLoopGroup eventLoopGroup,
        AtomicLong messageId,
        TwitterCredentials credentials
    ) {
        super("twitter", false, false, EnumSet.noneOf(ModerationOperation.class));
        this.notificationService = notificationService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.credentials = credentials;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, String name, String key, boolean outbound) {
        try {
            return new TwitterProxy(
                notificationService,
                messageBroadcaster,
                eventLoopGroup,
                this,
                id,
                remoteRoom,
                messageId,
                room,
                credentials
            );
        } catch (SSLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean validateCredentials(String name, String tokenPair) {
        return false;
    }
}
