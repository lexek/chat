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
public class ClearRoomHandler extends AbstractRoomMessageHandler {
    private final MessageBroadcaster messageBroadcaster;

    @Inject
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
        messageBroadcaster.submitMessage(Message.clearMessage(room.getName()), room.FILTER);
    }
}
