package lexek.wschat.chat.processing;

import lexek.wschat.chat.MessageProperty;
import lexek.wschat.chat.MessageType;

import java.util.Set;

public interface MessageHandler {
    Set<MessageProperty> requiredProperties();

    MessageType getType();

    boolean isNeedsInterval();
}
