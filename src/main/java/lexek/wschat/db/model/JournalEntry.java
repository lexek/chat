package lexek.wschat.db.model;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lexek.wschat.chat.model.User;

public class JournalEntry {
    private final User user;
    private final long time;
    private final String action;
    @JsonRawValue
    private final String actionDescription;
    private final User admin;
    private final Long roomId;

    public JournalEntry(User user, User admin, String action, String actionDescription, long time, Long roomId) {
        this.user = user;
        this.time = time;
        this.action = action;
        this.actionDescription = actionDescription;
        this.admin = admin;
        this.roomId = roomId;
    }

    public User getUser() {
        return user;
    }

    public long getTime() {
        return time;
    }

    public String getAction() {
        return action;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public User getAdmin() {
        return admin;
    }

    public Long getRoomId() {
        return roomId;
    }
}
