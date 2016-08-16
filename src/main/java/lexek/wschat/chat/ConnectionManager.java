package lexek.wschat.chat;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.netty.util.internal.RecyclableArrayList;
import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.User;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Service
public class ConnectionManager implements MessageEventHandler {
    private static final long FIVE_MIN_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    private final Set<ConnectionGroup> connectionGroups = new LinkedHashSet<>();

    @Inject
    public ConnectionManager(@Named("chatRegistry") MetricRegistry metricRegistry) {
        System.out.println("CONNECTION_MANAGER");
        metricRegistry.register("online", (Gauge<Map<String, Long>>) ConnectionManager.this::online);
    }

    @Inject
    public void init(IterableProvider<ConnectionGroup> connectionGroups) {
        connectionGroups.forEach(this::registerGroup);
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        BroadcastFilter<?> predicate = event.getBroadcastFilter();
        sendAll(event.getMessage(), predicate);
    }

    public void registerGroup(ConnectionGroup connectionGroup) {
        connectionGroups.add(connectionGroup);
    }

    private void sendAll(Message message, final Predicate<Connection> p) {
        for (ConnectionGroup group : connectionGroups) {
            group.send(message, p);
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
