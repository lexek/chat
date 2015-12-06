package lexek.wschat.chat.processing;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.User;

public interface RoomMessageHandler extends MessageHandler {
    boolean joinRequired();

    LocalRole getRole();

    void handle(Connection connection, User user, Room room, Chatter chatter, Message message);
}
