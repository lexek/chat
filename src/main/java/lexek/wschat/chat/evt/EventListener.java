package lexek.wschat.chat.evt;

import lexek.wschat.chat.Chatter;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;

@FunctionalInterface
public interface EventListener {
    void onEvent(Connection connection, Chatter chatter, Room room);
}
