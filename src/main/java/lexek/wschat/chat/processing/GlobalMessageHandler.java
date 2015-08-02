package lexek.wschat.chat.processing;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.chat.Message;
import lexek.wschat.chat.User;

public interface GlobalMessageHandler extends MessageHandler {
    GlobalRole getRole();

    void handle(Connection connection, User user, Message message);
}
