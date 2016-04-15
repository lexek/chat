package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.Message;

public class SendTopicOnEventListener implements EventListener {
    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        connection.send(Message.infoMessage(room.getTopic(), room.getName()));
    }
}
