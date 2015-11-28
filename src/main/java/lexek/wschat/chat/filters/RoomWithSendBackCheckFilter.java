package lexek.wschat.chat.filters;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;

public class RoomWithSendBackCheckFilter extends RoomFilter {
    private final Connection connection;

    public RoomWithSendBackCheckFilter(Room room, Connection connection) {
        super(room);
        this.connection = connection;
    }

    @Override
    public boolean test(Connection input) {
        boolean same = input == connection;
        boolean needSendingBack = input.isNeedSendingBack();
        if (needSendingBack) {
            return super.test(input);
        } else {
            return super.test(input) && !same;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RoomWithSendBackCheckFilter) {
            RoomWithSendBackCheckFilter other = (RoomWithSendBackCheckFilter) o;
            //yy reference checks
            return this.room == other.room && this.connection == other.connection;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "RoomWithSendBackCheckFilter{" +
            "connection=" + connection +
            "} " + super.toString();
    }
}
