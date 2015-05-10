package lexek.wschat.chat;

import lexek.wschat.services.Service;
import org.jetbrains.annotations.NotNull;

public interface MessageReactor extends Service {
    void registerHandler(@NotNull MessageHandler handler);

    void processMessage(Connection connection, InboundMessage message);
}
