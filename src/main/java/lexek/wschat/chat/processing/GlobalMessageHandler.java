package lexek.wschat.chat.processing;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.User;

public interface GlobalMessageHandler extends MessageHandler {
    GlobalRole getRole();

    void handle(Connection connection, User user, Message message);
}
