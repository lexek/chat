package lexek.wschat.proxy.twitter;

public interface TwitterMessageConsumer {
    ConsumerType getConsumerType();

    String getEntityName();

    void onTweet(Tweet tweet);
}
