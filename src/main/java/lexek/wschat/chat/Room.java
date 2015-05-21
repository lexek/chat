package lexek.wschat.chat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.db.dao.ChatterDao;
import lexek.wschat.services.JournalService;
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
    private final ChatterDao chatterDao;
    private final JournalService journalService;
    private final long id;
    private final String name;
    private String topic;

    public Room(UserService userService, JournalService journalService, ChatterDao chatterDao, long id, String name, String topic) {
        this.userService = userService;
        this.journalService = journalService;
        this.chatterDao = chatterDao;
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
                    chatter = chatterDao.getChatter(user, id);
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
                chatter = chatterDao.getChatter(user, id);
            } else {
                chatter = chatterDao.getChatter(userName, id);
            }
        }
        return chatter;
    }

    public static boolean canBan(Chatter modChatter, Chatter userChatter) {
        User user = userChatter.getUser();
        User modUser = modChatter.getUser();
        return userChatter.getRole() != LocalRole.ADMIN &&
                (
                        (
                                modChatter.hasRole(LocalRole.MOD) &&
                                        modChatter.hasGreaterRole(userChatter.getRole()) &&
                                        modUser.hasGreaterRole(user.getRole())
                        ) || (
                                user != null &&
                                        modUser.hasRole(GlobalRole.MOD) &&
                                        modUser.hasGreaterRole(user.getRole())
                        )
                );
    }

    public static boolean canChangeRole(Chatter modChatter, Chatter userChatter, LocalRole newRole) {
        User user = userChatter.getUser();
        User modUser = modChatter.getUser();
        return newRole != LocalRole.GUEST &&
                (
                        (
                                modChatter.hasRole(LocalRole.ADMIN) &&
                                        modChatter.hasGreaterRole(userChatter.getRole())
                        ) || (
                                user != null &&
                                        modUser.hasRole(GlobalRole.ADMIN) &&
                                        modUser.hasGreaterRole(user.getRole())
                        )
                );
    }

    public boolean banChatter(Chatter chatter, Chatter mod) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null) {
            result = chatterDao.banChatter(chatter.getId());
            if (result) {
                chatter.setBanned(true);
                journalService.roomBan(chatter.getUser().getWrappedObject(), mod.getUser().getWrappedObject(), this);
            }
        }
        return result;
    }

    public boolean unbanChatter(Chatter chatter, Chatter mod) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null) {
            result = chatterDao.unbanChatter(chatter.getId());
            if (result) {
                chatter.setBanned(false);
                chatter.setTimeout(null);
                journalService.roomUnban(chatter.getUser().getWrappedObject(), mod.getUser().getWrappedObject(), this);
            }
        }
        return result;
    }

    public boolean timeoutChatter(Chatter chatter, long until) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null) {
            result = chatterDao.timeoutChatter(chatter.getId(), until);
            if (result) {
                chatter.setTimeout(until);
            }
        }
        return result;
    }

    public boolean setRole(Chatter chatter, Chatter admin, LocalRole newRole) {
        boolean result = false;
        if (chatter != null && chatter.getId() != null && newRole != LocalRole.GUEST) {
            result = chatterDao.setRole(chatter.getId(), newRole);
            if (result) {
                chatter.setRole(newRole);
                journalService.roomRole(chatter.getUser().getWrappedObject(), admin.getUser().getWrappedObject(), this,
                        newRole);
            }
        }
        return result;
    }

    public boolean contains(Connection connection) {
        boolean result = false;
        readLock.lock();
        try {
            result = chatters.contains(connection);
        } finally {
            readLock.unlock();
        }
        return result;
    }

    public Set<Chatter> getChatters() {
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

    public void removeTimeout(Chatter chatter) {
        chatterDao.unbanChatter(chatter.getId());
        chatter.setTimeout(null);
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
}
