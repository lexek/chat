package lexek.wschat.db.model;

import lexek.wschat.chat.LocalRole;
import lexek.wschat.chat.User;
import org.jooq.Record;

import static lexek.wschat.db.jooq.tables.Chatter.CHATTER;

public class Chatter {
    public static final Chatter GUEST_CHATTER =
        new Chatter(null, LocalRole.GUEST, false, null, User.UNAUTHENTICATED_USER);

    private Long id;
    private LocalRole role;
    private boolean banned;
    private Long timeout;
    private User user;

    public Chatter(Long id, LocalRole role, boolean banned, Long timeout, User user) {
        this.id = id;
        this.role = role;
        this.banned = banned;
        this.timeout = timeout;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalRole getRole() {
        return role;
    }

    public void setRole(LocalRole role) {
        this.role = role;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean hasRole(LocalRole other) {
        return this.getRole().compareTo(other) >= 0;
    }

    public boolean hasGreaterRole(LocalRole other) {
        return this.getRole().compareTo(other) > 0;
    }

    public static Chatter fromRecord(Record record, User user) {
        if (record != null && record.getValue(CHATTER.ID) != null) {
            return new Chatter(
                record.getValue(CHATTER.ID),
                LocalRole.valueOf(record.getValue(CHATTER.ROLE)),
                record.getValue(CHATTER.BANNED),
                record.getValue(CHATTER.TIMEOUT),
                user
            );
        } else {
            return null;
        }
    }
}
