package lexek.wschat.db.model;

import lexek.wschat.chat.model.MessageType;

public class HistoryData {
    private final long id;
    private final String message;
    private final MessageType type;
    private final long timestamp;
    private final String userName;
    private final boolean hidden;
    private final boolean legacy;

    public HistoryData(long id, String message, MessageType type, long timestamp, String userName, boolean hidden, boolean legacy) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.userName = userName;
        this.hidden = hidden;
        this.legacy = legacy;
    }

    public long getId() {
        return id;
    }
    public String getMessage() {
        return message;
    }

    public MessageType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUserName() {
        return userName;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isLegacy() {
        return legacy;
    }
}
