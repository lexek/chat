package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.EventDispatcher;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class JoinHandler extends AbstractRoomMessageHandler {
    private final EventDispatcher notificationService;
    private final MessageBroadcaster messageBroadcaster;

    @Inject
    public JoinHandler(EventDispatcher notificationService, MessageBroadcaster messageBroadcaster) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM
            ),
            MessageType.JOIN,
            LocalRole.GUEST,
            false
        );
        this.notificationService = notificationService;
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    public boolean joinRequired() {
        return false;
    }

    @Override
    public void handle(Connection connection, User user, Room room, Chatter shouldBeNull, Message message) {
        if (room.inRoom(connection)) {
            connection.send(Message.errorMessage("ROOM_ALREADY_JOINED"));
            return;
        }
        Chatter chatter = room.join(connection);
        boolean sendJoin = !room.inRoom(user);
        Message joinMessage = Message.joinMessage(room.getName(), user, chatter.getRole());
        connection.send(Message.selfJoinMessage(room.getName(), chatter, room.getTopic()));
        if (sendJoin) {
            if (chatter.hasRole(LocalRole.USER)) {
                messageBroadcaster.submitMessage(joinMessage, room.FILTER);
            }
        }
        notificationService.joinedRoom(connection, chatter, room);
    }
}
