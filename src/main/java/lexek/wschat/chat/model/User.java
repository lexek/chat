package lexek.wschat.chat.model;

public interface User {
    String getColor();

    String getName();

    GlobalRole getRole();

    boolean isBanned();

    boolean isRenameAvailable();

    Long getId();

    default boolean hasRole(GlobalRole other) {
        return this.getRole().compareTo(other) >= 0;
    }

    default boolean hasGreaterRole(GlobalRole other) {
        return this.getRole().compareTo(other) > 0;
    }
}
