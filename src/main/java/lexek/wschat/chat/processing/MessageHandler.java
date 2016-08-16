package lexek.wschat.chat.processing;

import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.model.MessageType;
import org.jvnet.hk2.annotations.Contract;

import java.util.Set;

@Contract
public interface MessageHandler {
    Set<MessageProperty> requiredProperties();

    MessageType getType();

    boolean isNeedsInterval();
}
