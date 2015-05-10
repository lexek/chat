package lexek.wschat.chat;

public class MessageEvent {
    private Connection connection;
    private Message message;
    private BroadcastFilter broadcastFilter;

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public BroadcastFilter getBroadcastFilter() {
        return broadcastFilter;
    }

    public void setBroadcastFilter(BroadcastFilter broadcastFilter) {
        this.broadcastFilter = broadcastFilter;
    }
}
