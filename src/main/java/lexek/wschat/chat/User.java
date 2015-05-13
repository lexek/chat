package lexek.wschat.chat;

import lexek.wschat.db.model.UserDto;
import lexek.wschat.util.Colors;
import org.jetbrains.annotations.NotNull;

public class User {
    private static final UserDto userDto = new UserDto(
            null,
            "unauthenticated",
            GlobalRole.UNAUTHENTICATED,
            Colors.generateColor("unauthenticated"),
            false,
            false,
            null);
    public static final User UNAUTHENTICATED_USER = new User(userDto);

    private UserDto wrappedObject;
    private long lastMessage;

    public User(@NotNull UserDto wrappedObject) {
        this.wrappedObject = wrappedObject;
    }

    public String getColor() {
        return wrappedObject.getColor();
    }

    public void setColor(@NotNull String color) {
        this.wrappedObject.setColor(color);
    }

    public String getName() {
        return this.wrappedObject.getName();
    }

    public void setName(@NotNull String name) {
        this.wrappedObject.setName(name);
    }

    public GlobalRole getRole() {
        return this.wrappedObject.getRole();
    }

    public void setRole(@NotNull GlobalRole role) {
        this.wrappedObject.setRole(role);
    }

    public boolean isBanned() {
        return this.wrappedObject.isBanned();
    }

    public void setBanned(boolean banned) {
        this.wrappedObject.setBanned(banned);
    }

    public long getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(long lastMessage) {
        this.lastMessage = lastMessage;
    }

    public boolean isRenameAvailable() {
        return this.wrappedObject.isRenameAvailable();
    }

    public void setRenameAvailable(boolean renameAvailable) {
        this.wrappedObject.setRenameAvailable(renameAvailable);
    }

    public Long getId() {
        return this.wrappedObject.getId();
    }

    public void wrap(@NotNull UserDto object) {
        if (!object.getName().equals(this.wrappedObject.getName())) {
            throw new RuntimeException("Names should match.");
        }
        this.wrappedObject = object;
    }

    public UserDto getWrappedObject() {
        return this.wrappedObject;
    }

    public boolean hasRole(GlobalRole other) {
        return this.getRole().compareTo(other) >= 0;
    }

    public boolean hasGreaterRole(GlobalRole other) {
        return this.getRole().compareTo(other) > 0;
    }

    @Override
    public String toString() {
        return "User{" +
                "wrappedObject=" + wrappedObject +
                ", lastMessage=" + lastMessage +
                '}';
    }
}
