package lexek.wschat.chat;

import lexek.wschat.chat.model.Message;
import lexek.wschat.services.managed.ManagedService;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface MessageReactor extends ManagedService {
    void processMessage(Connection connection, Message message);
}
