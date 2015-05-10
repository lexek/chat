package lexek.wschat.chat;

import org.jetbrains.annotations.NotNull;

public class GlobalRoleFilter implements BroadcastFilter<GlobalRole> {
    private final GlobalRole role;

    public GlobalRoleFilter(GlobalRole role) {
        this.role = role;
    }

    @NotNull
    @Override
    public Type getType() {
        return Type.ROLE;
    }

    @Override
    public GlobalRole getData() {
        return role;
    }

    @Override
    public boolean test(Connection input) {
        return input.getUser().hasRole(role);
    }
}
