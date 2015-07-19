package lexek.wschat.chat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.db.model.Chatter;
import lexek.wschat.services.ChatterService;
import lexek.wschat.services.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Room {
    public final BroadcastFilter<Room> FILTER = new RoomFilter(this);
    private final Map<Long, Chatter> chatterCache = new ConcurrentHashMapV8<>();
    private final Multiset<User> onlineCounter = LinkedHashMultiset.create();
    private final Set<Connection> chatters = new HashSet<>();
    private final List<Message> history = new CopyOnWriteArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final UserService userService;
    private final ChatterService chatterService;
    private final long id;
    private final String name;
    private String topic;

    public Room(UserService userService, ChatterService chatterService, long id, String name, String topic) {
        this.userService = userService;
        this.chatterService = chatterService;
        this.id = id;
        this.name = name;
        this.topic = topic;
    }

    public Chatter getChatter(Long userId) {
        if (userId == null) {
            return Chatter.GUEST_CHATTER;
        } else {
            return chatterCache.get(userId);
        }
    }

    public Chatter getChatter(String name) {
        User user = userService.getCached(name);
        Chatter chatter = null;
        if (user != null) {
            chatter = chatterCache.get(user.getId());
        }
        return chatter;
    }

    public Chatter join(Connection connection) {
        Chatter chatter = null;
        writeLock.lock();
        try {
            chatters.add(connection);
            onlineCounter.add(connection.getUser());
            User user = connection.getUser();
            if (user == User.UNAUTHENTICATED_USER || user.getId() == null) {
                chatter = Chatter.GUEST_CHATTER;
            } else {
                chatter = chatterCache.get(user.getId());
                if (chatter == null) {
                    chatter = chatterService.getChatter(this, user);
                    chatterCache.put(user.getId(), chatter);
                }
            }
        } finally {
            writeLock.unlock();
        }
        return chatter;
    }

    /**
     * @return true if PART message is needed to be broadcasted
     */
    public boolean part(Connection connection) {
        boolean result = false;
        writeLock.lock();
        try {
            chatters.remove(connection);
            result = onlineCounter.remove(connection.getUser(), 1) == 1;
            User user = connection.getUser();
            if (user.getId() != null && result) {
                chatterCache.remove(user.getId());
            }
        } finally {
            writeLock.unlock();
        }
        return result;
    }

    public Chatter fetchChatter(String userName) {
        User user = userService.getCached(userName);
        Chatter chatter = null;
        if (user != null) {
            chatter = chatterCache.get(user.getId());
        }
        if (chatter == null) {
            if (user != null) {
                chatter = chatterService.getChatter(this, user);
            } else {
                chatter = chatterService.getChatter(this, userName);
            }
        }
        return chatter;
    }

    public boolean inRoom(Connection connection) {
        boolean result = false;
        readLock.lock();
        try {
            result = chatters.contains(connection);
        } finally {
            readLock.unlock();
        }
        return result;
    }

    public Set<Chatter> getOnlineChatters() {
        Set<Chatter> result = null;
        readLock.lock();
        try {
            result = ImmutableSet.copyOf(chatterCache.values());
        } finally {
            readLock.unlock();
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getId() {
        return id;
    }

    public List<Message> getHistory() {
        return history;
    }

    public boolean hasUser(User user) {
        boolean result = false;
        readLock.lock();
        try {
            result = onlineCounter.count(user) > 0;
        } finally {
            readLock.unlock();
        }
        return result;
    }

    public long getOnline() {
        long result = 0;
        readLock.lock();
        try {
            for (Multiset.Entry<User> entry : onlineCounter.entrySet()) {
                if (entry.getElement().hasRole(GlobalRole.USER)) {
                    result += entry.getCount();
                }
            }
        } finally {
            readLock.unlock();
        }
        return result;
    }

    public void removeTimeout(Chatter chatter) {
        chatterService.removeTimeout(chatter);
    }

    public boolean timeoutChatter(Chatter chatter, long until) {
        return chatterService.timeoutChatter(this, chatter, until);
    }

    public boolean banChatter(Chatter user, Chatter mod) {
        return chatterService.banChatter(this, user, mod);
    }

    public boolean setRole(Chatter userChatter, Chatter adminChatter, LocalRole newRole) {
        return chatterService.setRole(this, userChatter, adminChatter, newRole);
    }

    public boolean unbanChatter(Chatter user, Chatter mod) {
        return chatterService.unbanChatter(this, user, mod);
    }
}
