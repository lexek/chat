package lexek.wschat.chat;

import org.jetbrains.annotations.NotNull;

public class UserIdFilter implements BroadcastFilter<Long> {
    private final Long id;

    public UserIdFilter(Long id) {
        this.id = id;
    }

    @NotNull
    @Override
    public Type getType() {
        return Type.CUSTOM;
    }

    @Override
    public Long getData() {
        return id;
    }

    @Override
    public boolean test(Connection input) {
        return id.equals(input.getUser().getId());
    }
}
