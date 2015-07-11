package lexek.wschat.db.model;

import lexek.wschat.chat.GlobalRole;
import org.jooq.Record;

import java.io.Serializable;
import java.security.Principal;

import static lexek.wschat.db.jooq.tables.User.USER;

public class UserDto implements Serializable, Principal {
    private Long id;
    private String name;
    private GlobalRole role;
    private String color;
    private boolean banned;
    private boolean renameAvailable;
    private String email;

    public UserDto(Long id, String name, GlobalRole role, String color, boolean banned, boolean renameAvailable, String email) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.color = color;
        this.banned = banned;
        this.renameAvailable = renameAvailable;
        this.email = email;
    }

    public static UserDto fromRecord(Record record) {
        if (record != null && record.getValue(USER.ID) != null) {
            return new UserDto(
                record.getValue(USER.ID),
                record.getValue(USER.NAME),
                GlobalRole.valueOf(record.getValue(USER.ROLE)),
                record.getValue(USER.COLOR),
                record.getValue(USER.BANNED),
                record.getValue(USER.RENAME_AVAILABLE),
                record.getValue(USER.EMAIL)
            );
        } else {
            return null;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GlobalRole getRole() {
        return role;
    }

    public void setRole(GlobalRole role) {
        this.role = role;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public boolean isRenameAvailable() {
        return renameAvailable;
    }

    public void setRenameAvailable(boolean renameAvailable) {
        this.renameAvailable = renameAvailable;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "UserDto{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", role=" + role +
            ", color='" + color + '\'' +
            ", banned=" + banned +
            ", renameAvailable=" + renameAvailable +
            ", email=" + email +
            '}';
    }

    public boolean hasRole(GlobalRole other) {
        return this.role.compareTo(other) >= 0;
    }
}
