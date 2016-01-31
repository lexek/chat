package lexek.wschat.proxy.twitter;

public class SimplifiedTweet {
    private final long id;
    private final String from;
    private final String text;
    private final SimplifiedTweet retweetedStatus;
    private final SimplifiedTweet quotedStatus;
    private final SimplifiedTweet replyToStatus;

    public SimplifiedTweet(long id, String from, String text) {
        this.id = id;
        this.from = from;
        this.text = text;
        this.retweetedStatus = null;
        this.quotedStatus = null;
        this.replyToStatus = null;
    }

    public SimplifiedTweet(
        long id,
        String from,
        String text,
        SimplifiedTweet retweetedStatus,
        SimplifiedTweet quotedStatus,
        SimplifiedTweet replyToStatus
    ) {
        this.id = id;
        this.from = from;
        this.text = text;
        this.retweetedStatus = retweetedStatus;
        this.quotedStatus = quotedStatus;
        this.replyToStatus = replyToStatus;
    }

    public long getId() {
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

    @Override
    public String toString() {
        return "SimplifiedTweet{" +
            "id=" + id +
            ", from='" + from + '\'' +
            ", text='" + text + '\'' +
            ", retweetedStatus=" + retweetedStatus +
            ", quotedStatus=" + quotedStatus +
            ", replyToStatus=" + replyToStatus +
            '}';
    }
}
