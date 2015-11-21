package lexek.wschat.chat.filters;

import lexek.wschat.chat.Connection;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public interface BroadcastFilter<T> extends Predicate<Connection> {
    BroadcastFilter<Void> NO_FILTER = new NoFilter();

    @NotNull
    Type getType();

    T getData();

    enum Type {
        ROOM,
        USER,
        ROLE,
        ALL,
        CUSTOM
    }
}
