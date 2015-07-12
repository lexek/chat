package lexek.wschat.services;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.db.model.Chatter;

@FunctionalInterface
public interface RoomJoinedEventListener {
    void joined(Connection connection, Chatter chatter, Room room);
}
