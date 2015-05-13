package lexek.wschat.db.model;

import lexek.wschat.chat.LocalRole;

public class ChatterData {
    private final long id;
    private final long userId;
    private final String userName;
    private final long roomId;
    private final LocalRole role;
    private final Long timeout;
    private final boolean banned;

    public ChatterData(long id, long userId, String userName, long roomId, LocalRole role, Long timeout, boolean banned) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.roomId = roomId;
        this.role = role;
        this.timeout = timeout;
        this.banned = banned;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public long getRoomId() {
        return roomId;
    }

    public LocalRole getRole() {
        return role;
    }

    public Long getTimeout() {
        return timeout;
    }

    public boolean isBanned() {
        return banned;
    }
}
