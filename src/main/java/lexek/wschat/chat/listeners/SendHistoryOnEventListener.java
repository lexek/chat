package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Chatter;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Message;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.EventListener;

public class SendHistoryOnEventListener implements EventListener {
    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        connection.send(Message.historyMessage(room.getHistory()));
    }
}
