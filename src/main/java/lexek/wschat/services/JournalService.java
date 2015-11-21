package lexek.wschat.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.LocalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.db.dao.JournalDao;
import lexek.wschat.db.jooq.tables.pojos.Announcement;
import lexek.wschat.db.model.Emoticon;
import lexek.wschat.db.model.JournalEntry;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.UserChangeSet;
import lexek.wschat.services.poll.Poll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalService {
    private final Logger logger = LoggerFactory.getLogger(JournalService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JournalDao journalDao;

    public JournalService(JournalDao journalDao) {
        this.journalDao = journalDao;
    }

    public void nameChanged(UserDto user, String oldName, String newName) {
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

    public void userUpdated(UserDto user, UserDto admin, UserChangeSet changeSet) {
        try {
            String description = objectMapper.writeValueAsString(ImmutableMap.of(
                "oldState", user,
                "newState", changeSet));
            journalDao.add(new JournalEntry(user, admin, "USER_UPDATE", description, now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void newEmoticon(UserDto admin, Emoticon emoticon) {
        try {
            journalDao.add(new JournalEntry(null, admin, "NEW_EMOTICON", objectMapper.writeValueAsString(emoticon), now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void deletedEmoticon(UserDto admin, Emoticon emoticon) {
        try {
            journalDao.add(new JournalEntry(null, admin, "DELETED_EMOTICON", objectMapper.writeValueAsString(emoticon), now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void newRoom(UserDto admin, Room room) {
        try {
            String description = objectMapper.writeValueAsString(ImmutableMap.of("name", room.getName()));
            journalDao.add(new JournalEntry(null, admin, "NEW_ROOM", description, now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void deletedRoom(UserDto admin, Room room) {
        try {
            String description = objectMapper.writeValueAsString(ImmutableMap.of("name", room.getName()));
            journalDao.add(new JournalEntry(null, admin, "DELETED_ROOM", description, now(), null));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void newPoll(UserDto admin, Room room, Poll poll) {
        try {
            journalDao.add(new JournalEntry(null, admin, "NEW_POLL", objectMapper.writeValueAsString(poll.getQuestion()), now(), room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void closedPoll(UserDto admin, Room room, Poll poll) {
        try {
            journalDao.add(new JournalEntry(null, admin, "CLOSE_POLL", objectMapper.writeValueAsString(poll.getQuestion()), now(), room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void roomBan(UserDto user, UserDto admin, Room room) {
        journalDao.add(new JournalEntry(user, admin, "ROOM_BAN", null, now(), room.getId()));
    }

    public void roomUnban(UserDto user, UserDto admin, Room room) {
        journalDao.add(new JournalEntry(user, admin, "ROOM_UNBAN", null, now(), room.getId()));
    }

    public void roomRole(UserDto user, UserDto admin, Room room, LocalRole role) {
        try {
            journalDao.add(new JournalEntry(user, admin, "ROOM_ROLE", objectMapper.writeValueAsString(role.toString()), now(), room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void newAnnouncement(UserDto admin, Room room, Announcement announcement) {
        try {
            journalDao.add(new JournalEntry(null, admin, "NEW_ANNOUNCEMENT", objectMapper.writeValueAsString(announcement.getText()), now(), room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void inactiveAnnouncement(UserDto admin, Room room, Announcement announcement) {
        try {
            journalDao.add(new JournalEntry(null, admin, "INACTIVE_ANNOUNCEMENT", objectMapper.writeValueAsString(announcement.getText()), now(), room.getId()));
        } catch (JsonProcessingException e) {
            logger.warn("", e);
        }
    }

    public void newProxy(UserDto admin, Room room, String providerName, String remoteRoom) {
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

    public void deletedProxy(UserDto admin, Room room, String providerName, String remoteRoom) {
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

    private long now() {
        return System.currentTimeMillis();
    }
}
