package lexek.wschat.chat.evt;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.Chatter;

@FunctionalInterface
public interface EventListener {
    void onEvent(Connection connection, Chatter chatter, Room room);
}
