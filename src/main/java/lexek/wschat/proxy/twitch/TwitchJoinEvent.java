package lexek.wschat.proxy.twitch;

public class TwitchJoinEvent extends TwitchEventMessage {
    private final String room;

    public TwitchJoinEvent(String data, String room) {
        super(Type.JOIN, data);
        this.room = room;
    }

    public String getRoom() {
        return room;
    }
}
