package lexek.wschat.services;

import lexek.wschat.chat.Chatter;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;

@FunctionalInterface
public interface RoomJoinedEventListener {
    void joined(Connection connection, Chatter chatter, Room room);
}
