package lexek.wschat.proxy.goodgame;

public class GoodGameEvent {
    private final GoodGameEventType type;
    private final String channel;
    private final String text;
    private final String user;

    public GoodGameEvent(GoodGameEventType type, String channel, String text, String user) {
        this.type = type;
        this.channel = channel;
        this.text = text;
        this.user = user;
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
}
