package lexek.wschat.chat;

import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.db.dao.ChatterDao;
import lexek.wschat.db.dao.RoomDao;
import lexek.wschat.services.UserService;

import java.util.Collection;
import java.util.Map;

public class RoomManager {
    private final Map<String, Room> rooms = new ConcurrentHashMapV8<>();
    private final Map<Long, Room> roomIds = new ConcurrentHashMapV8<>();
    private final MessageBroadcaster messageBroadcaster;
    private final RoomDao roomDao;
    private final UserService userService;
    private final ChatterDao chatterDao;

    public RoomManager(UserService userService, MessageBroadcaster messageBroadcaster, RoomDao roomDao, ChatterDao chatterDao) {
        this.messageBroadcaster = messageBroadcaster;
        this.roomDao = roomDao;
        this.userService = userService;
        this.chatterDao = chatterDao;

        for (lexek.wschat.db.jooq.tables.pojos.Room room : roomDao.getAll()) {
            Room instance = new Room(userService, chatterDao, room.getId(), room.getName(), room.getTopic());
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

    public void createRoom(lexek.wschat.db.jooq.tables.pojos.Room room) {
        if (!rooms.containsKey(room.getName())) {
            roomDao.add(room);
            rooms.put(room.getName(), new Room(userService, chatterDao, room.getId(), room.getName(), room.getTopic()));
        }
    }

    public void deleteRoom(String name) {
        if (!name.equals("#main")) {
            roomDao.delete(name);
            Room room = rooms.remove(name);
            roomIds.remove(room.getId());
        }
    }

    public Collection<Room> getRooms() {
        return this.rooms.values();
    }
}
