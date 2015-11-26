package lexek.wschat.db.model;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;

public class ChatterData {
    private final long id;
    private final long userId;
    private final String userName;
    private final LocalRole role;
    private final GlobalRole globalRole;
    private final boolean timedOut;
    private final boolean banned;

    public ChatterData(long id, long userId, String userName, LocalRole role, GlobalRole globalRole, boolean timedOut, boolean banned) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.role = role;
        this.globalRole = globalRole;
        this.timedOut = timedOut;
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

    public LocalRole getRole() {
        return role;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public boolean isBanned() {
        return banned;
    }

    public GlobalRole getGlobalRole() {
        return globalRole;
    }
}
