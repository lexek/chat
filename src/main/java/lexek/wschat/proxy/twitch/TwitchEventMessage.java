package lexek.wschat.proxy.twitch;

public class TwitchEventMessage {
    private final Type type;
    private final String data;

    public TwitchEventMessage(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "TwitchEventMessage{" +
            "type=" + type +
            ", data='" + data + '\'' +
            '}';
    }

    public enum Type {
        MSG,
        CLEAR,
        LOGIN_FAILED
    }
}
