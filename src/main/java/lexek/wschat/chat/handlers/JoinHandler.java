package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.db.model.Chatter;
import lexek.wschat.services.RoomJoinNotificationService;

import java.util.List;

public class JoinHandler extends AbstractMessageHandler {
    private final RoomJoinNotificationService notificationService;
    private final RoomManager roomManager;
    private final MessageBroadcaster messageBroadcaster;

    public JoinHandler(RoomJoinNotificationService notificationService,
                       RoomManager roomManager,
                       MessageBroadcaster messageBroadcaster) {
        super(MessageType.JOIN, GlobalRole.UNAUTHENTICATED, 1, false, true);
        this.notificationService = notificationService;
        this.roomManager = roomManager;
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    public void handle(List<String> args, Connection connection) {
        final Room room = roomManager.getRoomInstance(args.get(0));
        if (room != null && !room.contains(connection)) {
            User user = connection.getUser();
            boolean sendJoin = !room.hasUser(user);
            Chatter chatter = room.join(connection);

            Message joinMessage = Message.joinMessage(room.getName(), user.getWrappedObject());
            connection.send(Message.selfJoinMessage(room.getName(), chatter));
            if (sendJoin) {
                if (chatter.hasRole(LocalRole.USER)) {
                    messageBroadcaster.submitMessage(joinMessage, connection, room.FILTER);
                }
            }
            notificationService.joinedRoom(connection, chatter, room);
        } else {
            if (room == null) {
                connection.send(Message.errorMessage("ROOM_NOT_FOUND", ImmutableList.of(args.get(0))));
            } else {
                connection.send(Message.errorMessage("ROOM_ALREADY_JOINED"));
            }
        }
    }
}
