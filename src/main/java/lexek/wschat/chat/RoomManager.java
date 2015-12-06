package lexek.wschat.chat;

import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.db.dao.RoomDao;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.ChatterService;
import lexek.wschat.services.JournalService;
import lexek.wschat.services.UserService;

import java.util.Collection;
import java.util.Map;

public class RoomManager {
    private final Map<String, Room> rooms = new ConcurrentHashMapV8<>();
    private final Map<Long, Room> roomIds = new ConcurrentHashMapV8<>();
    private final MessageBroadcaster messageBroadcaster;
    private final ChatterService chatterService;
    private final RoomDao roomDao;
    private final UserService userService;
    private final JournalService journalService;

    public RoomManager(UserService userService, MessageBroadcaster messageBroadcaster, RoomDao roomDao,
                       ChatterService chatterService, JournalService journalService) {
        this.messageBroadcaster = messageBroadcaster;
        this.roomDao = roomDao;
        this.userService = userService;
        this.chatterService = chatterService;
        this.journalService = journalService;

        for (lexek.wschat.db.jooq.tables.pojos.Room room : roomDao.getAll()) {
            Room instance = new Room(userService, chatterService, room.getId(), room.getName(), room.getTopic());
            rooms.put(room.getName(), instance);
            roomIds.put(room.getId(), instance);
        }
    }

    public Room getRoomInstance(String name) {
        return rooms.get(name);
    }

    public Room getRoomInstance(long id) {
        return roomIds.get(id);
    }

    public void partAll(Connection connection, boolean sendPartMessage) {
        rooms.values()
            .stream()
            .filter(room -> room.inRoom(connection) &&
                room.part(connection) &&
                connection.getUser().hasRole(GlobalRole.USER) &&
                sendPartMessage)
            .forEach(room -> messageBroadcaster.submitMessage(
                Message.partMessage(room.getName(), connection.getUser().getName()), room.FILTER)
            );
    }

    public void createRoom(String name, String topic, UserDto admin) {
        if (!rooms.containsKey(name)) {
            lexek.wschat.db.jooq.tables.pojos.Room pojo = new lexek.wschat.db.jooq.tables.pojos.Room(null, name, topic);
            roomDao.add(pojo);
            Room room = new Room(userService, chatterService, pojo.getId(), pojo.getName(), pojo.getTopic());
            rooms.put(pojo.getName(), room);
            roomIds.put(room.getId(), room);
            journalService.newRoom(admin, room);
        }
    }

    public void updateTopic(UserDto admin, Room room, String newTopic) {
        roomDao.updateTopic(room.getId(), newTopic);
        journalService.topicChanged(admin, room, newTopic);
        room.setTopic(newTopic);
    }

    public void deleteRoom(Room room, UserDto admin) {
        if (!room.getName().equals("#main")) {
            roomDao.delete(room.getId());
            roomIds.remove(room.getId());
            rooms.remove(room.getName());
            journalService.deletedRoom(admin, room);
        }
    }

    public Collection<Room> getRooms() {
        return this.rooms.values();
    }
}
