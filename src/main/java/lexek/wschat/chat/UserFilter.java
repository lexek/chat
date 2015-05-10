package lexek.wschat.chat;

import org.jetbrains.annotations.NotNull;

public class UserFilter implements BroadcastFilter<User> {
    private final User user;

    public UserFilter(User user) {
        this.user = user;
    }

    @NotNull
    @Override
    public Type getType() {
        return Type.USER;
    }

    @Override
    public User getData() {
        return user;
    }

    @Override
    public boolean test(Connection input) {
        User user1 = input.getUser();
        return (user.getId() != null) && (user1.getId() != null) && (user1.getId().equals(user.getId()));
    }
}
