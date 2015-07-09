package lexek.wschat.db.model;

import com.fasterxml.jackson.annotation.JsonRawValue;

public class JournalEntry {
    private final UserDto user;
    private final long time;
    private final String action;
    @JsonRawValue
    private final String actionDescription;
    private final UserDto admin;
    private final Long roomId;

    public JournalEntry(UserDto user, UserDto admin, String action, String actionDescription, long time, Long roomId) {
        this.user = user;
        this.time = time;
        this.action = action;
        this.actionDescription = actionDescription;
        this.admin = admin;
        this.roomId = roomId;
    }

    public UserDto getUser() {
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

    public UserDto getAdmin() {
        return admin;
    }

    public Long getRoomId() {
        return roomId;
    }
}
