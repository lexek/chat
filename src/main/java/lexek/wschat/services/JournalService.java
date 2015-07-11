package lexek.wschat.services;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import lexek.wschat.chat.LocalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.db.dao.JournalDao;
import lexek.wschat.db.jooq.tables.pojos.Announcement;
import lexek.wschat.db.jooq.tables.pojos.Emoticon;
import lexek.wschat.db.model.JournalEntry;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.UserChangeSet;

public class JournalService {
    private final Gson gson = new Gson();
    private final JournalDao journalDao;

    public JournalService(JournalDao journalDao) {
        this.journalDao = journalDao;
    }

    public void nameChanged(UserDto user, String oldName, String newName) {
        String description = gson.toJson(ImmutableMap.of(
            "oldName", oldName,
            "newName", newName
        ));
        journalDao.add(new JournalEntry(user, null, "NAME_CHANGE", description, now(), null));
    }

    public void userUpdated(UserDto user, UserDto admin, UserChangeSet changeSet) {
        String description = gson.toJson(ImmutableMap.of(
            "oldState", gson.toJsonTree(user),
            "newState", gson.toJsonTree(changeSet)));
        journalDao.add(new JournalEntry(user, admin, "USER_UPDATE", description, now(), null));
    }

    public void newEmoticon(UserDto admin, Emoticon emoticon) {
        journalDao.add(new JournalEntry(null, admin, "NEW_EMOTICON", gson.toJson(emoticon), now(), null));
    }

    public void deletedEmoticon(UserDto admin, Emoticon emoticon) {
        journalDao.add(new JournalEntry(null, admin, "DELETED_EMOTICON", gson.toJson(emoticon), now(), null));
    }

    public void newRoom(UserDto admin, Room room) {
        journalDao.add(new JournalEntry(null, admin, "NEW_ROOM", room.getName(), now(), room.getId()));
    }

    public void deletedRoom(UserDto admin, Room room) {
        journalDao.add(new JournalEntry(null, admin, "DELETED_ROOM", room.getName(), now(), room.getId()));
    }

    public void newPoll(UserDto admin, Room room, Poll poll) {
        journalDao.add(new JournalEntry(null, admin, "NEW_POLL", poll.getQuestion(), now(), room.getId()));
    }

    public void closedPoll(UserDto admin, Room room, Poll poll) {
        journalDao.add(new JournalEntry(null, admin, "CLOSE_POLL", poll.getQuestion(), now(), room.getId()));
    }

    public void roomBan(UserDto user, UserDto admin, Room room) {
        journalDao.add(new JournalEntry(user, admin, "ROOM_BAN", null, now(), room.getId()));
    }

    public void roomUnban(UserDto user, UserDto admin, Room room) {
        journalDao.add(new JournalEntry(user, admin, "ROOM_UNBAN", null, now(), room.getId()));
    }

    public void roomRole(UserDto user, UserDto admin, Room room, LocalRole role) {
        journalDao.add(new JournalEntry(user, admin, "ROOM_ROLE", role.toString(), now(), room.getId()));
    }

    public void newAnnouncement(UserDto admin, Room room, Announcement announcement) {
        journalDao.add(new JournalEntry(null, admin, "NEW_ANNOUNCEMENT", announcement.getText(), now(), room.getId()));
    }

    public void inactiveAnnouncement(UserDto admin, Room room, Announcement announcement) {
        journalDao.add(new JournalEntry(null, admin, "INACTIVE_ANNOUNCEMENT", announcement.getText(), now(), room.getId()));
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
