package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;

import java.util.List;

public class PartHandler extends AbstractMessageHandler {
    private final MessageBroadcaster messageBroadcaster;
    private final RoomManager roomManager;

    public PartHandler(MessageBroadcaster messageBroadcaster, RoomManager roomManager) {
        super(MessageType.PART, GlobalRole.UNAUTHENTICATED, 1, false, true);

        this.messageBroadcaster = messageBroadcaster;
        this.roomManager = roomManager;
    }

    @Override
    public void handle(List<String> args, Connection connection) {
        final Room room = roomManager.getRoomInstance(args.get(0));
        if (room != null && room.contains(connection)) {
            final User user = connection.getUser();
            if (room.part(connection) && user.hasRole(GlobalRole.USER)) {
                Message message = Message.partMessage(room.getName(), user.getName());
                messageBroadcaster.submitMessage(message, connection, room.FILTER);
            }
        } else {
            if (room == null) {
                connection.send(Message.errorMessage("ROOM_NOT_FOUND", ImmutableList.of(args.get(0))));
            } else {
                connection.send(Message.errorMessage("ROOM_NOT_JOINED"));
            }
        }
    }
}
