package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class LikeHandler extends AbstractRoomMessageHandler {
    private final MessageBroadcaster messageBroadcaster;

    @Inject
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
                room.FILTER);
        }
    }
}
