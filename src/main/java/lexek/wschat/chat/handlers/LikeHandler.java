package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;

public class LikeHandler extends AbstractRoomMessageHandler {
    private final MessageBroadcaster messageBroadcaster;

    public LikeHandler(MessageBroadcaster messageBroadcaster) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM,
                MessageProperty.MESSAGE_ID
            ),
            MessageType.LIKE,
            LocalRole.USER,
            true
        );
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    public void handle(Connection connection, User user, Room room, Chatter chatter, Message message) {
        Long id = message.get(MessageProperty.MESSAGE_ID);
        if (id != null) {
            messageBroadcaster.submitMessage(
                Message.likeMessage(
                    room.getName(),
                    connection.getUser().getName(),
                    id
                ),
                connection,
                room.FILTER);
        }
    }
}
