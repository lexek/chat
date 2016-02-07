package lexek.wschat.proxy.twitter;

import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.proxy.ProxyState;
import lexek.wschat.util.Colors;

import java.util.concurrent.atomic.AtomicLong;

public class TwitterProxy implements Proxy, TwitterMessageConsumer {
    private final long id;
    private final TwitterStreamingClient twitterClient;
    private final String remoteRoom;
    private final ProxyProvider provider;
    private final ConsumerType consumerType;
    private final String entityName;
    private final Room room;
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private volatile boolean running;

    public TwitterProxy(
        MessageBroadcaster messageBroadcaster,
        TwitterStreamingClient twitterClient,
        AtomicLong messageId,
        Room room,
        ProxyProvider provider,
        long id,
        String remoteRoom,
        ConsumerType consumerType
    ) {
        this.id = id;
        this.twitterClient = twitterClient;
        this.remoteRoom = remoteRoom;
        this.provider = provider;
        this.room = room;
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.entityName = remoteRoom;
        this.consumerType = consumerType;
    }

    @Override
    public synchronized void start() {
        running = true;
        twitterClient.registerConsumer(this);
    }

    @Override
    public synchronized void stop() {
        running = false;
        twitterClient.deregisterConsumer(this);
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMessage(Message message) {
        //ignore
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public ProxyProvider provider() {
        return provider;
    }

    @Override
    public String remoteRoom() {
        return remoteRoom;
    }

    @Override
    public boolean outboundEnabled() {
        return false;
    }

    @Override
    public boolean moderationEnabled() {
        return false;
    }

    @Override
    public ProxyState state() {
        return running ? twitterClient.state() : ProxyState.STOPPED;
    }

    @Override
    public String lastError() {
        return twitterClient.lastError();
    }

    public ConsumerType getConsumerType() {
        return consumerType;
    }

    @Override
    public String getEntityName() {
        return entityName;
    }

    @Override
    public void onTweet(SimplifiedTweet tweet) {
        //todo: implement custom message type
        Message msg = Message.extMessage(
            room.getName(),
            tweet.getFrom(),
            LocalRole.USER,
            GlobalRole.USER,
            Colors.generateColor(tweet.getFrom()),
            messageId.getAndIncrement(),
            System.currentTimeMillis(),
            tweet.getText(),
            "twitter",
            "twitter"
        );
        messageBroadcaster.submitMessage(msg, room.FILTER);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TwitterProxy that = (TwitterProxy) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
