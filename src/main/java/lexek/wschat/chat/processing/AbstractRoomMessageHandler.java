package lexek.wschat.chat.processing;

import lexek.wschat.chat.LocalRole;
import lexek.wschat.chat.MessageProperty;
import lexek.wschat.chat.MessageType;

import java.util.Set;

public abstract class AbstractRoomMessageHandler implements RoomMessageHandler {
    private final Set<MessageProperty> requiredProperties;
    private final MessageType type;
    private final LocalRole role;
    private final boolean needsInterval;

    public AbstractRoomMessageHandler(Set<MessageProperty> requiredProperties, MessageType type, LocalRole role, boolean needsInterval) {
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
    public LocalRole getRole() {
        return role;
    }

    @Override
    public boolean isNeedsInterval() {
        return needsInterval;
    }

    @Override
    public boolean joinRequired() {
        return true;
    }

    @Override
    public Set<MessageProperty> requiredProperties() {
        return requiredProperties;
    }
}
