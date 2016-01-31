package lexek.wschat.proxy.twitter;

import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.services.NotificationService;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class TwitterProxyProvider extends ProxyProvider {
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final TwitterStreamingClient twitterClient;

    public TwitterProxyProvider(
        NotificationService notificationService,
        MessageBroadcaster messageBroadcaster,
        EventLoopGroup eventLoopGroup,
        AtomicLong messageId,
        TwitterCredentials credentials
    ) {
        super("twitter", false, false, EnumSet.noneOf(ModerationOperation.class));
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.twitterClient = new TwitterStreamingClient(notificationService, eventLoopGroup, this, credentials);
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, String name, String key, boolean outbound) {
        if (remoteRoom.startsWith("#")) {
            return new TwitterProxy(
                messageBroadcaster,
                twitterClient,
                messageId,
                room,
                this,
                id,
                remoteRoom.substring(1),
                ConsumerType.TWEETS_HASHTAG
            );
        }
        if (remoteRoom.startsWith("link:")) {
            return new TwitterProxy(
                messageBroadcaster,
                twitterClient,
                messageId,
                room,
                this,
                id,
                remoteRoom.substring(5),
                ConsumerType.TWEETS_LINK
            );
        }
        if (remoteRoom.startsWith("text:")) {
            return new TwitterProxy(
                messageBroadcaster,
                twitterClient,
                messageId,
                room,
                this,
                id,
                remoteRoom.substring(5),
                ConsumerType.TWEETS_PHRASE
            );
        }
        throw new UnsupportedOperationException("couldn't detect type");
    }

    @Override
    public boolean validateCredentials(String name, String tokenPair) {
        return false;
    }
}
