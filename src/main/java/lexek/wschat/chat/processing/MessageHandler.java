package lexek.wschat.chat.processing;

import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.model.MessageType;

import java.util.Set;

public interface MessageHandler {
    Set<MessageProperty> requiredProperties();

    MessageType getType();

    boolean isNeedsInterval();
}
