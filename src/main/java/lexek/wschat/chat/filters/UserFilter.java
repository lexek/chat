package lexek.wschat.chat.filters;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.model.User;
import org.jetbrains.annotations.NotNull;

public class UserFilter implements BroadcastFilter<Long> {
    private final Long userId;

    public UserFilter(User user) {
        this.userId = user.getId();
    }

    public UserFilter(Long userId) {
        this.userId = userId;
    }

    @NotNull
    @Override
    public Type getType() {
        return Type.USER;
    }

    @Override
    public Long getData() {
        return userId;
    }

    @Override
    public boolean test(Connection input) {
        User user1 = input.getUser();
        return (userId == null && user1.getId() == null) ||
            (userId != null) && (user1.getId() != null) && (user1.getId().equals(userId));
    }
}
