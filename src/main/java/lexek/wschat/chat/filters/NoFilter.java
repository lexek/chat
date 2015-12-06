package lexek.wschat.chat.filters;

import lexek.wschat.chat.Connection;
import org.jetbrains.annotations.NotNull;

public class NoFilter implements BroadcastFilter<Void> {
    @NotNull
    @Override
    public Type getType() {
        return Type.ALL;
    }

    @Override
    public Void getData() {
        return null;
    }

    @Override
    public boolean test(Connection input) {
        return true;
    }
}
