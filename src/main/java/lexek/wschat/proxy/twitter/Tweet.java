package lexek.wschat.proxy.twitter;

import lexek.wschat.chat.model.MessageProperty;

public class Tweet {
    public static final MessageProperty<Tweet> TWEET_PROPERTY = MessageProperty.valueOf("tweet");
    private final String id;
    private final String from;
    private final String fromFullName;
    private final String fromAvatar;
    private final String text;
    private final long when;
    private final Tweet retweetedStatus;
    private final Tweet quotedStatus;
    private final Tweet replyToStatus;

    public Tweet(String id, String from, String text) {
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

    public Tweet(
        String id,
        String from,
        String fromFullName, String fromAvatar, String text,
        long when, Tweet retweetedStatus,
        Tweet quotedStatus,
        Tweet replyToStatus
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

    public Tweet getRetweetedStatus() {
        return retweetedStatus;
    }

    public Tweet getQuotedStatus() {
        return quotedStatus;
    }

    public Tweet getReplyToStatus() {
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
        return "Tweet{" +
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
