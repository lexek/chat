package lexek.wschat.chat.processing;

import lexek.wschat.chat.*;

public interface RoomMessageHandler extends MessageHandler {
    boolean joinRequired();

    LocalRole getRole();

    void handle(Connection connection, User user, Room room, Chatter chatter, Message message);
}
