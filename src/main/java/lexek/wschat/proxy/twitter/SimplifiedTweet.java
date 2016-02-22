package lexek.wschat.proxy.twitter;

import lexek.wschat.chat.model.MessageProperty;

public class SimplifiedTweet {
    public static final MessageProperty<SimplifiedTweet> TWEET_PROPERTY = MessageProperty.valueOf("tweet");
    private final String id;
    private final String from;
    private final String fromFullName;
    private final String fromAvatar;
    private final String text;
    private final long when;
    private final SimplifiedTweet retweetedStatus;
    private final SimplifiedTweet quotedStatus;
    private final SimplifiedTweet replyToStatus;

    public SimplifiedTweet(String id, String from, String text) {
        this.id = id;
        this.from = from;
        this.text = text;
        this.fromAvatar = null;
        this.fromFullName = null;
        this.retweetedStatus = null;
        this.quotedStatus = null;
        this.replyToStatus = null;
        this.when = -1;
    }

    public SimplifiedTweet(
        String id,
        String from,
        String fromFullName, String fromAvatar, String text,
        long when, SimplifiedTweet retweetedStatus,
        SimplifiedTweet quotedStatus,
        SimplifiedTweet replyToStatus
    ) {
        this.id = id;
        this.from = from;
        this.fromFullName = fromFullName;
        this.fromAvatar = fromAvatar;
        this.text = text;
        this.when = when;
        this.retweetedStatus = retweetedStatus;
        this.quotedStatus = quotedStatus;
        this.replyToStatus = replyToStatus;
    }

    public String getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public String getText() {
        return text;
    }

    public SimplifiedTweet getRetweetedStatus() {
        return retweetedStatus;
    }

    public SimplifiedTweet getQuotedStatus() {
        return quotedStatus;
    }

    public SimplifiedTweet getReplyToStatus() {
        return replyToStatus;
    }

    public long getWhen() {
        return when;
    }

    public String getFromAvatar() {
        return fromAvatar;
    }

    public String getFromFullName() {
        return fromFullName;
    }

    @Override
    public String toString() {
        return "SimplifiedTweet{" +
            "id='" + id + '\'' +
            ", from='" + from + '\'' +
            ", fromFullName='" + fromFullName + '\'' +
            ", fromAvatar='" + fromAvatar + '\'' +
            ", text='" + text + '\'' +
            ", when=" + when +
            ", retweetedStatus=" + retweetedStatus +
            ", quotedStatus=" + quotedStatus +
            ", replyToStatus=" + replyToStatus +
            '}';
    }
}
