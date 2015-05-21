package lexek.wschat.chat;

import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.db.dao.ChatterDao;
import lexek.wschat.db.dao.RoomDao;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.JournalService;
import lexek.wschat.services.UserService;

import java.util.Collection;
import java.util.Map;

public class RoomManager {
    private final Map<String, Room> rooms = new ConcurrentHashMapV8<>();
    private final Map<Long, Room> roomIds = new ConcurrentHashMapV8<>();
    private final MessageBroadcaster messageBroadcaster;
    private final ChatterDao chatterDao;
    private final RoomDao roomDao;
    private final UserService userService;
    private final JournalService journalService;

    public RoomManager(UserService userService, MessageBroadcaster messageBroadcaster, RoomDao roomDao,
                       ChatterDao chatterDao, JournalService journalService) {
        this.messageBroadcaster = messageBroadcaster;
        this.roomDao = roomDao;
        this.userService = userService;
        this.chatterDao = chatterDao;
        this.journalService = journalService;

        for (lexek.wschat.db.jooq.tables.pojos.Room room : roomDao.getAll()) {
            Room instance = new Room(userService, journalService, chatterDao, room.getId(), room.getName(), room.getTopic());
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
                .filter(room -> room.contains(connection) &&
                        room.part(connection) &&
                        connection.getUser().hasRole(GlobalRole.USER) &&
                        sendPartMessage)
                .forEach(room -> messageBroadcaster.submitMessage(
                        Message.partMessage(room.getName(), connection.getUser().getName()), connection, room.FILTER));
    }

    public void createRoom(lexek.wschat.db.jooq.tables.pojos.Room roomPojo, UserDto admin) {
        if (!rooms.containsKey(roomPojo.getName())) {
            roomDao.add(roomPojo);
            Room room = new Room(userService, journalService, chatterDao, roomPojo.getId(), roomPojo.getName(), roomPojo.getTopic());
            rooms.put(roomPojo.getName(), room);
            journalService.newRoom(admin, room);
        }
    }

    public void deleteRoom(String name, UserDto admin) {
        if (!name.equals("#main")) {
            roomDao.delete(name);
            Room room = rooms.remove(name);
            roomIds.remove(room.getId());
            journalService.deletedRoom(admin, room);
        }
    }

    public Collection<Room> getRooms() {
        return this.rooms.values();
    }
}
