package lexek.wschat.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.dao.JournalDao;
import lexek.wschat.db.jooq.tables.pojos.Announcement;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.Emoticon;
import lexek.wschat.db.model.JournalEntry;
import lexek.wschat.db.model.form.UserChangeSet;
import lexek.wschat.services.poll.Poll;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JournalService {
    private static final Map<String, Set<String>> GLOBAL_CATEGORIES = ImmutableMap.of(
        "user_self", ImmutableSet.of("NAME_CHANGE", "USER_CREATED", "PASSWORD_RESET"),
        "user_admin", ImmutableSet.of("USER_UPDATE", "PASSWORD"),
        "emoticon", ImmutableSet.of("NEW_EMOTICON", "IMAGE_EMOTICON", "DELETED_EMOTICON"),
        "room_admin", ImmutableSet.of("NEW_ROOM", "DELETE_ROOM")
    );
    private static final Map<String, Set<String>> ROOM_CATEGORIES = ImmutableMap.<String, Set<String>>builder()
        .put("poll", ImmutableSet.of("NEW_POLL", "CLOSE_POLL"))
        .put("ban", ImmutableSet.of("ROOM_BAN", "ROOM_UNBAN"))
        .put("role", ImmutableSet.of("ROOM_ROLE"))
        .put("announcement", ImmutableSet.of("NEW_ANNOUNCEMENT", "INACTIVE_ANNOUNCEMENT"))
        .put("proxy", ImmutableSet.of("NEW_PROXY", "DELETED_PROXY"))
        .put("topic", ImmutableSet.of("TOPIC_CHANGED"))
        .build();

    private final Logger logger = LoggerFactory.getLogger(JournalService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JournalDao journalDao;

    @Inject
    public JournalService(JournalDao journalDao) {
        this.journalDao = journalDao;
    }

    public void nameChanged(User user, String oldName, String newName) {
        try {
            String description = objectMapper.writeValueAsString(ImmutableMap.of(
                "oldName", oldName,
                "newName", newName
            ));
            journalDao.add(new JournalEntry(user, null, "NAME_CHANGE", description, now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void userCreated(User user) {
        journalDao.add(new JournalEntry(user, null, "USER_CREATED", null, now(), null));
    }

    public void userUpdated(User user, User admin, UserChangeSet changeSet) {
        try {
            String description = objectMapper.writeValueAsString(ImmutableMap.of(
                "oldState", user,
                "newState", changeSet
            ));
            journalDao.add(new JournalEntry(user, admin, "USER_UPDATE", description, now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void userPasswordChanged(User admin, User user) {
        journalDao.add(new JournalEntry(
            user,
            admin,
            "PASSWORD",
            null,
            now(),
            null
        ));
    }

    public void userPasswordReset(User user) {
        journalDao.add(new JournalEntry(
            user,
            null,
            "PASSWORD_RESET",
            null,
            now(),
            null
        ));
    }

    public void newEmoticon(User admin, Emoticon emoticon) {
        try {
            journalDao.add(new JournalEntry(null, admin, "NEW_EMOTICON", objectMapper.writeValueAsString(emoticon), now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void emoticonImageChanged(User admin, String code, String oldFile, String newFile) {
        try {
            String data = objectMapper.writeValueAsString(ImmutableMap.of(
                "code", code,
                "oldImage", oldFile,
                "newImage", newFile
            ));
            journalDao.add(new JournalEntry(null, admin, "IMAGE_EMOTICON", data, now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void deletedEmoticon(User admin, Emoticon emoticon) {
        try {
            journalDao.add(new JournalEntry(null, admin, "DELETED_EMOTICON", objectMapper.writeValueAsString(emoticon), now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void newRoom(User admin, Room room) {
        try {
            String description = objectMapper.writeValueAsString(ImmutableMap.of("name", room.getName()));
            journalDao.add(new JournalEntry(null, admin, "NEW_ROOM", description, now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void deletedRoom(User admin, Room room) {
        try {
            String description = objectMapper.writeValueAsString(ImmutableMap.of("name", room.getName()));
            journalDao.add(new JournalEntry(null, admin, "DELETED_ROOM", description, now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void newPoll(User admin, Room room, Poll poll) {
        try {
            journalDao.add(new JournalEntry(null, admin, "NEW_POLL", objectMapper.writeValueAsString(poll.getQuestion()), now(), room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void closedPoll(User admin, Room room, Poll poll) {
        try {
            journalDao.add(new JournalEntry(null, admin, "CLOSE_POLL", objectMapper.writeValueAsString(poll.getQuestion()), now(), room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void roomBan(User user, User admin, Room room) {
        journalDao.add(new JournalEntry(user, admin, "ROOM_BAN", null, now(), room.getId()));
    }

    public void roomUnban(User user, User admin, Room room) {
        journalDao.add(new JournalEntry(user, admin, "ROOM_UNBAN", null, now(), room.getId()));
    }

    public void roomRole(User user, User admin, Room room, LocalRole role) {
        try {
            journalDao.add(new JournalEntry(user, admin, "ROOM_ROLE", objectMapper.writeValueAsString(role.toString()), now(), room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void newAnnouncement(User admin, Room room, Announcement announcement) {
        try {
            journalDao.add(new JournalEntry(null, admin, "NEW_ANNOUNCEMENT", objectMapper.writeValueAsString(announcement.getText()), now(), room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void inactiveAnnouncement(User admin, Room room, Announcement announcement) {
        try {
            journalDao.add(new JournalEntry(null, admin, "INACTIVE_ANNOUNCEMENT", objectMapper.writeValueAsString(announcement.getText()), now(), room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void newProxy(User admin, Room room, String providerName, String remoteRoom) {
        try {
            journalDao.add(new JournalEntry(
                null,
                admin,
                "NEW_PROXY",
                objectMapper.writeValueAsString(ImmutableMap.of(
                    "providerName", providerName,
                    "remoteRoom", remoteRoom
                )),
                now(),
                room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void deletedProxy(User admin, Room room, String providerName, String remoteRoom) {
        try {
            journalDao.add(new JournalEntry(
                null,
                admin,
                "DELETED_PROXY",
                objectMapper.writeValueAsString(ImmutableMap.of(
                    "providerName", providerName,
                    "remoteRoom", remoteRoom
                )),
                now(),
                room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void topicChanged(User admin, Room room, String newTopic) {
        try {
            journalDao.add(new JournalEntry(
                null,
                admin,
                "TOPIC_CHANGED",
                objectMapper.writeValueAsString(ImmutableMap.of(
                    "oldTopic", room.getTopic(),
                    "newTopic", newTopic
                )),
                now(),
                room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public DataPage<JournalEntry> getRoomJournal(
        int page,
        int pageSize,
        long roomId,
        Optional<Set<String>> categories,
        Optional<Long> userId,
        Optional<Long> adminId
    ) {
        return journalDao.fetchAllForRoom(
            page,
            pageSize,
            roomId,
            getTypes(categories, ROOM_CATEGORIES),
            userId,
            adminId
        );
    }

    public DataPage<JournalEntry> getGlobalJournal(
        int page,
        int pageSize,
        Optional<Set<String>> categories,
        Optional<Long> userId,
        Optional<Long> adminId
    ) {
        return journalDao.fetchAllGlobal(
            page,
            pageSize,
            getTypes(categories, GLOBAL_CATEGORIES),
            userId,
            adminId
        );
    }

    private Optional<Set<String>> getTypes(Optional<Set<String>> selectedCategories, Map<String, Set<String>> categories) {
        return selectedCategories
            .map(cats -> cats.stream()
                .filter(categories::containsKey)
                .flatMap(e -> categories.get(e).stream())
                .collect(Collectors.toSet())
            );
    }

    private long now() {
        return System.currentTimeMillis();
    }

    public Map<String, Set<String>> getGlobalCategories() {
        return GLOBAL_CATEGORIES;
    }

    public Map<String, Set<String>> getRoomCategories() {
        return ROOM_CATEGORIES;
    }
}
