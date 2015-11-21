package lexek.wschat.chat.filters;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.User;
import org.jetbrains.annotations.NotNull;

public class UserInRoomFilter implements BroadcastFilter<Void> {
    private final Long userId;
    private final Room room;

    public UserInRoomFilter(User user, Room room) {
        this.userId = user.getId();
        this.room = room;
    }

    @NotNull
    @Override
    public Type getType() {
        return Type.CUSTOM;
    }

    @Override
    public Void getData() {
        return null;
    }

    @Override
    public boolean test(Connection connection) {
        User user1 = connection.getUser();
        boolean userMatch = (userId == null && user1.getId() == null) ||
            (userId != null) && (user1.getId() != null) && (user1.getId().equals(userId));
        return userMatch && room.inRoom(connection);
    }
}
