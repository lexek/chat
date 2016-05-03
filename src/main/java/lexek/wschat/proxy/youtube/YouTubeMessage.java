package lexek.wschat.proxy.youtube;

public class YouTubeMessage {
    private final String name;
    private final String message;
    private final long publishedAt;

    public YouTubeMessage(String name, String message, long publishedAt) {
        this.name = name;
        this.message = message;
        this.publishedAt = publishedAt;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public long getPublishedAt() {
        return publishedAt;
    }
}
