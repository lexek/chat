package lexek.wschat.services;

import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.db.model.UserDto;

import java.util.Collection;

public class RoomService {
    private final RoomManager roomManager;

    public RoomService(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    public Room getRoomInstance(String name) {
        Room room = roomManager.getRoomInstance(name);
        if (room != null) {
            return room;
        } else {
            throw new EntityNotFoundException("Room not found");
        }
    }

    public Room getRoomInstance(long id) {
        Room room = roomManager.getRoomInstance(id);
        if (room != null) {
            return room;
        } else {
            throw new EntityNotFoundException("Room not found.");
        }
    }


    public Collection<Room> getRooms() {
        return roomManager.getRooms();
    }

    public void deleteRoom(Room room, UserDto admin) {
        roomManager.deleteRoom(room, admin);
    }

    public void createRoom(String name, String topic, UserDto admin) {
        roomManager.createRoom(name, topic, admin);
    }

    public void updateTopic(UserDto admin, Room room, String topic) {
        roomManager.updateTopic(admin, room, topic);
    }
}
