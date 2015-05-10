package lexek.wschat.chat;

import org.jetbrains.annotations.NotNull;

public class RoomFilter implements BroadcastFilter<Room> {
    private final Room room;

    public RoomFilter(Room room) {
        this.room = room;
    }

    @NotNull
    @Override
    public Type getType() {
        return Type.ROOM;
    }

    @Override
    public Room getData() {
        return room;
    }

    @Override
    public boolean test(Connection input) {
        return room.contains(input);
    }
}
