package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.chat.Chatter;

import java.util.List;

abstract class AbstractMsgHandler extends AbstractMessageHandler {
    private final RoomManager roomManager;

    AbstractMsgHandler(MessageType type, int argCount, boolean needsInterval, RoomManager roomManager) {
        super(type, GlobalRole.USER, argCount, needsInterval, true);

        this.roomManager = roomManager;
    }

    @Override
    final public void handle(final List<String> args, final Connection connection) {
        Room room = roomManager.getRoomInstance(args.get(0));
        if (room != null) {
            if (room.inRoom(connection)) {
                Chatter chatter = room.getChatter(connection.getUser().getId());
                if (!(chatter.getUser().hasRole(GlobalRole.MOD) || chatter.hasRole(LocalRole.MOD))) {
                    if (chatter.isBanned() || connection.getUser().isBanned()) {
                        connection.send(Message.errorMessage("BAN"));
                        return;
                    } else if (chatter.getTimeout() != null) {
                        if (chatter.getTimeout() < System.currentTimeMillis()) {
                            room.removeTimeout(chatter);
                            handle(args, connection);
                            return;
                        } else {
                            long remainingTime = (chatter.getTimeout() - System.currentTimeMillis()) / 1000;
                            Message msg = Message.errorMessage("TIMEOUT", ImmutableList.of(remainingTime));
                            connection.send(msg);
                            return;
                        }
                    }
                }
                handle(connection, room, chatter, args);
            } else {
                connection.send(Message.errorMessage("NOT_JOINED"));
            }
        } else {
            connection.send(Message.errorMessage("UNKNOWN_ROOM"));
        }
    }

    abstract protected void handle(Connection connection, Room room, Chatter chatter, List<String> args);
}
