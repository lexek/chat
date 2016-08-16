package lexek.wschat.chat;

import lexek.wschat.chat.model.Message;
import org.jvnet.hk2.annotations.Contract;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Contract
public interface ConnectionGroup<T extends Connection> {
    void send(Message message, Predicate<Connection> predicate);

    void registerConnection(T connection);

    void deregisterConnection(T connection);

    void forEach(Consumer<Connection> function);

    void forEach(Predicate<Connection> filter, Consumer<Connection> function);

    long count();

    void getConnections(List<Connection> connections);

    boolean anyConnection(Predicate<Connection> connectionPredicate);
}
