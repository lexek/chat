package lexek.wschat.frontend.irc;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.ConnectionGroup;
import lexek.wschat.chat.model.Message;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Service
public class IrcConnectionGroup implements ConnectionGroup<IrcConnection> {
    private final IrcCodec codec;
    private final Set<IrcConnection> connections = new LinkedHashSet<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    @Inject
    public IrcConnectionGroup(IrcCodec codec) {
        this.codec = codec;
    }

    @Override
    public void registerConnection(IrcConnection c) {
        writeLock.lock();
        try {
            connections.add(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void deregisterConnection(IrcConnection c) {
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
    public void send(Message message, Predicate<Connection> predicate) {
        String encodedMessage = codec.encode(message);

        if (encodedMessage != null) {
            readLock.lock();
            try {
                connections.stream().filter(predicate).forEach(c -> c.send(encodedMessage));
            } finally {
                readLock.unlock();
            }
        }
    }
}
