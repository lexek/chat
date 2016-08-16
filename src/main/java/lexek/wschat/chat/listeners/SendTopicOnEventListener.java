package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.ChatEventType;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.Message;
import org.jvnet.hk2.annotations.Service;

@Service
public class SendTopicOnEventListener implements EventListener {
    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        connection.send(Message.infoMessage(room.getTopic(), room.getName()));
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public ChatEventType getEventType() {
        return ChatEventType.JOIN;
    }
}
