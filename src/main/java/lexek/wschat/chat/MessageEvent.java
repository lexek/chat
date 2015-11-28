package lexek.wschat.chat;

import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;

public class MessageEvent {
    private Message message;
    private BroadcastFilter broadcastFilter;

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
