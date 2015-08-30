package lexek.wschat.proxy.goodgame;

public class GoodGameEvent {
    private final GoodGameEventType type;
    private final String channel;
    private final String text;
    private final String user;
    private final String id;

    public GoodGameEvent(GoodGameEventType type, String channel, String text, String user, String id) {
        this.type = type;
        this.channel = channel;
        this.text = text;
        this.user = user;
        this.id = id;
    }

    public GoodGameEventType getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }

    public String getUser() {
        return user;
    }

    public String getText() {
        return text;
    }

    public String getId() {
        return id;
    }
}
