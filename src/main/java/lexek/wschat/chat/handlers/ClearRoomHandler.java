package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;

public class ClearRoomHandler extends AbstractRoomMessageHandler {
    private final MessageBroadcaster messageBroadcaster;

    public ClearRoomHandler(MessageBroadcaster messageBroadcaster) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM
            ),
            MessageType.CLEAR_ROOM,
            LocalRole.MOD,
            false
        );
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    public void handle(Connection connection, User user, Room room, Chatter chatter, Message message) {
        messageBroadcaster.submitMessage(Message.clearMessage(room.getName()), connection, room.FILTER);
    }
}
