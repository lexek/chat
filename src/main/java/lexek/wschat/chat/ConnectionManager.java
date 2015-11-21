package lexek.wschat.chat;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.lmax.disruptor.EventHandler;
import io.netty.util.internal.RecyclableArrayList;
import lexek.wschat.chat.filters.BroadcastFilter;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ConnectionManager implements EventHandler<MessageEvent> {
    private static final long FIVE_MIN_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    private final Set<ConnectionGroup> connectionGroups = new LinkedHashSet<>();

    public ConnectionManager(MetricRegistry metricRegistry) {
        metricRegistry.register("online", (Gauge<Map<String, Long>>) ConnectionManager.this::online);
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        BroadcastFilter<?> predicate = event.getBroadcastFilter();
        if (predicate != null) {
            sendAll(event.getMessage(), event.getConnection(), predicate);
        } else {
            sendAll(event.getMessage(), event.getConnection());
        }
    }

    public void registerGroup(ConnectionGroup connectionGroup) {
        connectionGroups.add(connectionGroup);
    }

    private void sendAll(Message message, final Connection connection) {
        if (connection.isNeedSendingBack()) {
            for (ConnectionGroup c : connectionGroups) {
                c.send(message, connection.getUser());
            }
        } else {
            for (ConnectionGroup group : connectionGroups) {
                group.send(message, connection.getUser(), c -> (connection != c));
            }
        }
    }

    private void sendAll(Message message, final Connection connection, final Predicate<Connection> p) {
        if (connection.isNeedSendingBack()) {
            for (ConnectionGroup c : connectionGroups) {
                c.send(message, connection.getUser(), p);
            }
        } else {
            for (ConnectionGroup group : connectionGroups) {
                group.send(message, connection.getUser(), p.and(c -> (connection != c)));
            }
        }
    }

    public void forEach(Consumer<Connection> function) {
        for (ConnectionGroup c : connectionGroups) {
            c.forEach(function);
        }
    }

    public void forEach(Predicate<Connection> filter, Consumer<Connection> function) {
        for (ConnectionGroup c : connectionGroups) {
            c.forEach(filter, function);
        }
    }

    public Map<String, Long> online() {
        long online = connectionGroups.stream().mapToLong(ConnectionGroup::count).sum();

        long authenticated = 0;
        long active = 0;
        RecyclableArrayList connections = RecyclableArrayList.newInstance();
        for (ConnectionGroup connectionGroup : connectionGroups) {
            connectionGroup.getConnections(connections);
        }

        Set<User> checkedUsers = new HashSet<>();

        for (Object connection : connections) {
            User user = ((Connection) connection).getUser();
            if (!checkedUsers.contains(user)) {
                if (user.hasRole(GlobalRole.USER)) {
                    authenticated++;
                    if (System.currentTimeMillis() - user.getLastMessage() < FIVE_MIN_MS) {
                        active++;
                    }
                }
                checkedUsers.add(user);
            }
        }
        connections.recycle();

        Map<String, Long> result = new HashMap<>();
        result.put("online", online);
        result.put("authenticated", authenticated);
        result.put("active", active);

        return result;
    }

    public List<Connection> getConnections() {
        List<Connection> connections = new ArrayList<>();
        for (ConnectionGroup c : connectionGroups) {
            c.getConnections(connections);
        }
        return connections;
    }

    public boolean anyConnection(Predicate<Connection> connectionPredicate) {
        return connectionGroups.stream().anyMatch(group -> group.anyConnection(connectionPredicate));
    }
}
