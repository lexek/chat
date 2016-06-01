package lexek.wschat.db.model;

import com.fasterxml.jackson.annotation.JsonView;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.frontend.http.rest.view.DetailView;
import lexek.wschat.frontend.http.rest.view.MessageView;
import lexek.wschat.frontend.http.rest.view.SimpleView;
import org.jooq.Record;

import java.io.Serializable;
import java.security.Principal;

import static lexek.wschat.db.jooq.tables.User.USER;

public class UserDto implements Serializable, Principal {
    @JsonView({DetailView.class, SimpleView.class})
    private Long id;
    @JsonView({MessageView.class, DetailView.class, SimpleView.class})
    private String name;
    @JsonView({MessageView.class, DetailView.class})
    private GlobalRole role;
    @JsonView({MessageView.class, DetailView.class})
    private String color;
    @JsonView({MessageView.class, DetailView.class})
    private boolean banned;
    @JsonView(DetailView.class)
    private boolean renameAvailable;
    @JsonView(DetailView.class)
    private String email;
    @JsonView(DetailView.class)
    private boolean emailVerified;
    @JsonView(DetailView.class)
    private boolean checkIp;

    public UserDto(
        Long id,
        String name,
        GlobalRole role,
        String color,
        boolean banned,
        boolean renameAvailable,
        String email,
        boolean emailVerified,
        boolean checkIp
    ) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.color = color;
        this.banned = banned;
        this.renameAvailable = renameAvailable;
        this.email = email;
        this.emailVerified = emailVerified;
        this.checkIp = checkIp;
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
                record.getValue(USER.EMAIL),
                record.getValue(USER.EMAIL_VERIFIED),
                record.getValue(USER.CHECK_IP));
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

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isCheckIp() {
        return checkIp;
    }

    public void setCheckIp(boolean checkIp) {
        this.checkIp = checkIp;
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
            ", emailVerified=" + emailVerified +
            '}';
    }

    public boolean hasRole(GlobalRole other) {
        return this.role.compareTo(other) >= 0;
    }
}
