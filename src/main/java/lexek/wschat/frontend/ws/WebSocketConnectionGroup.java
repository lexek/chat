package lexek.wschat.frontend.ws;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.frontend.Codec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WebSocketConnectionGroup implements ConnectionGroup<WebSocketConnectionAdapter> {
    private static final ImmutableSet<MessageType> IGNORE_TYPES = ImmutableSet.of(MessageType.JOIN, MessageType.PART);
    private final Codec codec;
    private final Set<WebSocketConnectionAdapter> connections = new HashSet<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public WebSocketConnectionGroup(Codec codec) {
        this.codec = codec;
    }

    @Override
    public void registerConnection(WebSocketConnectionAdapter c) {
        writeLock.lock();
        try {
            connections.add(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void deregisterConnection(WebSocketConnectionAdapter c) {
        writeLock.lock();
        try {
            connections.remove(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void forEach(Consumer<Connection> function) {
        readLock.lock();
        try {
            connections.forEach(function);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void forEach(Predicate<Connection> filter, Consumer<Connection> function) {
        readLock.lock();
        try {
            connections.stream().filter(filter).forEach(function);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long count() {
        readLock.lock();
        long result = 0;
        try {
            result = connections.size();
        } finally {
            readLock.unlock();
        }
        return result;
    }

    @Override
    public void getConnections(List<Connection> c) {
        readLock.lock();
        try {
            c.addAll(connections);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean anyConnection(Predicate<Connection> connectionPredicate) {
        boolean result = false;
        readLock.lock();
        try {
            result = connections.stream().anyMatch(connectionPredicate);
        } finally {
            readLock.unlock();
        }
        return result;
    }

    @Override
    public void send(Message message, User user) {
        if (!IGNORE_TYPES.contains(message.getType())) {
            readLock.lock();
            try {
                String encodedMessage = codec.encode(message, user);
                for (WebSocketConnectionAdapter c : connections) {
                    c.send(encodedMessage);
                }
            } finally {
                readLock.unlock();
            }
        }
    }

    @Override
    public void send(Message message, User user, Predicate<Connection> predicate) {
        if (!IGNORE_TYPES.contains(message.getType())) {
            readLock.lock();
            try {
                String encodedMessage = codec.encode(message, user);
                connections.stream().filter(predicate).forEach(c -> c.send(encodedMessage));
            } finally {
                readLock.unlock();
            }
        }
    }
}
