package lexek.wschat.proxy.twitter;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.services.NotificationService;

import java.util.EnumSet;
import java.util.Set;

public class TwitterProxyProvider extends ProxyProvider {
    private static final Set<String> PREFIXES = ImmutableSet.of("@", "$", "#", "link:", "text:");
    private final MessageBroadcaster messageBroadcaster;
    private final TwitterStreamingClient twitterClient;
    private final TwitterApiClient profileSource;

    public TwitterProxyProvider(
        NotificationService notificationService,
        MessageBroadcaster messageBroadcaster,
        EventLoopGroup eventLoopGroup,
        TwitterCredentials credentials,
        TwitterApiClient profileSource
    ) {
        super("twitter", false, false, EnumSet.noneOf(ModerationOperation.class));
        this.messageBroadcaster = messageBroadcaster;
        this.profileSource = profileSource;
        this.twitterClient = new TwitterStreamingClient(notificationService, eventLoopGroup, this, profileSource, credentials);
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, String name, String key, boolean outbound) {
        if (remoteRoom.startsWith("#")) {
            return new TwitterProxy(
                messageBroadcaster,
                twitterClient,
                room,
                this,
                id,
                remoteRoom.substring(1).toLowerCase(),
                ConsumerType.TWEETS_HASHTAG
            );
        }
        if (remoteRoom.startsWith("$")) {
            return new TwitterProxy(
                messageBroadcaster,
                twitterClient,
                room,
                this,
                id,
                remoteRoom.substring(1).toLowerCase(),
                ConsumerType.TWEETS_SYMBOL
            );
        }
        if (remoteRoom.startsWith("link:")) {
            return new TwitterProxy(
                messageBroadcaster,
                twitterClient,
                room,
                this,
                id,
                remoteRoom.substring(5).toLowerCase(),
                ConsumerType.TWEETS_LINK
            );
        }
        if (remoteRoom.startsWith("text:")) {
            return new TwitterProxy(
                messageBroadcaster,
                twitterClient,
                room,
                this,
                id,
                remoteRoom.substring(5).toLowerCase(),
                ConsumerType.TWEETS_PHRASE
            );
        }
        if (remoteRoom.startsWith("@")) {
            return new TwitterProxy(
                messageBroadcaster,
                twitterClient,
                room,
                this,
                id,
                remoteRoom.substring(1).toLowerCase(),
                ConsumerType.TWEETS_ACCOUNT
            );
        }
        throw new UnsupportedOperationException("couldn't detect type");
    }

    @Override
    public boolean validateCredentials(String name, String tokenPair) {
        return false;
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        if (remoteRoom.startsWith("@")) {
            ProfileSummary profileSummary = profileSource.getProfileSummary(remoteRoom.substring(1));
            return !profileSummary.isProtected();
        } else {
            return PREFIXES.stream().anyMatch(remoteRoom::startsWith);
        }
    }
}
