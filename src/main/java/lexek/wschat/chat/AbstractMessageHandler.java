package lexek.wschat.chat;

public abstract class AbstractMessageHandler implements MessageHandler {
    private final MessageType type;
    private final GlobalRole role;
    private final int argCount;
    private final boolean needsInterval;
    private final boolean needsLogging;

    public AbstractMessageHandler(MessageType type, GlobalRole role, int argCount, boolean needsInterval, boolean needsLogging) {
        this.type = type;
        this.role = role;
        this.argCount = argCount;
        this.needsInterval = needsInterval;
        this.needsLogging = needsLogging;
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
    public int getArgCount() {
        return argCount;
    }

    @Override
    public boolean isNeedsInterval() {
        return needsInterval;
    }

    @Override
    public boolean isNeedsLogging() {
        return needsLogging;
    }
}
