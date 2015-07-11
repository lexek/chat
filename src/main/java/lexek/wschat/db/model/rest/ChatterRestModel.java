package lexek.wschat.db.model.rest;

import lexek.wschat.chat.LocalRole;

public class ChatterRestModel {
    private final long id;
    private final long userId;
    private final String name;
    private final boolean timedOut;
    private final boolean banned;
    private final LocalRole role;

    public ChatterRestModel(long id, long userId, String name, boolean timedOut, boolean banned, LocalRole role) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.timedOut = timedOut;
        this.banned = banned;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public boolean isBanned() {
        return banned;
    }

    public LocalRole getRole() {
        return role;
    }
}
