package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;

public class PartHandler extends AbstractRoomMessageHandler {
    private final MessageBroadcaster messageBroadcaster;

    public PartHandler(MessageBroadcaster messageBroadcaster) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM
            ),
            MessageType.PART,
            LocalRole.GUEST,
            true
        );

        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    public void handle(Connection connection, User user, Room room, Chatter chatter, Message message) {
        if (room.part(connection) && user.hasRole(GlobalRole.USER)) {
            messageBroadcaster.submitMessage(Message.partMessage(room.getName(), user.getName()), connection, room.FILTER);
        }
    }
}
