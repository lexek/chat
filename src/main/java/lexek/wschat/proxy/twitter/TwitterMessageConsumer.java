package lexek.wschat.proxy.twitter;

public interface TwitterMessageConsumer {
    ConsumerType getConsumerType();

    String getEntityName();

    String getDestination();

    void onTweet(Tweet tweet);
}
