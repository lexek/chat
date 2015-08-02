package lexek.wschat.chat.processing;

import lexek.wschat.chat.GlobalRole;
import lexek.wschat.chat.MessageProperty;
import lexek.wschat.chat.MessageType;

import java.util.Set;

public abstract class AbstractGlobalMessageHandler implements GlobalMessageHandler {
    private final Set<MessageProperty> requiredProperties;
    private final MessageType type;
    private final GlobalRole role;
    private final boolean needsInterval;

    public AbstractGlobalMessageHandler(Set<MessageProperty> requiredProperties, MessageType type, GlobalRole role, boolean needsInterval) {
        this.requiredProperties = requiredProperties;
        this.type = type;
        this.role = role;
        this.needsInterval = needsInterval;
    }

    @Override
    public MessageType getType() {
        return type;
    }

    @Override
    public GlobalRole getRole() {
        return role;
    }

    @Override
    public boolean isNeedsInterval() {
        return needsInterval;
    }

    @Override
    public Set<MessageProperty> requiredProperties() {
        return requiredProperties;
    }
}
