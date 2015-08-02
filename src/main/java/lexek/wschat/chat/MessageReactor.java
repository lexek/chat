package lexek.wschat.chat;

import lexek.wschat.services.Service;

public interface MessageReactor extends Service {
    void processMessage(Connection connection, Message message);
}
