package lexek.wschat.proxy.twitch;

public class TwitchUserMessage extends TwitchEventMessage {
    private final TwitchUser user;

    public TwitchUserMessage(String data, TwitchUser user) {
        super(Type.MSG, data);
        this.user = user;
    }

    public TwitchUser getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "TwitchUserMessage{" +
            "user=" + user +
            "} " + super.toString();
    }
}
