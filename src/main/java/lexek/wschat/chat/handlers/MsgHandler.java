package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.filters.RoomWithSendBackCheckFilter;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MsgHandler extends AbstractRoomMessageHandler {
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;

    @Inject
    public MsgHandler(@Named("messageId") AtomicLong messageId, MessageBroadcaster messageBroadcaster) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM,
                MessageProperty.TEXT
            ),
            MessageType.MSG,
            LocalRole.USER,
            true);
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    public void handle(Connection connection, User user, Room room, Chatter chatter, Message message) {
        String text = message.getText().trim();
        if (text.length() == 0) {
            connection.send(Message.errorMessage("EMPTY_MESSAGE"));
        } else if (!(chatter.hasRole(LocalRole.MOD) || connection.getUser().hasRole(GlobalRole.MOD)) && (text.length() > 420)) {
            connection.send(Message.errorMessage("MESSAGE_TOO_BIG"));
        } else {
            messageBroadcaster.submitMessage(
                Message.msgMessage(
                    room.getName(),
                    chatter.getUser().getId(),
                    chatter.getUser().getName(),
                    chatter.getRole(),
                    chatter.getUser().getRole(),
                    chatter.getUser().getColor(),
                    messageId.getAndIncrement(),
                    System.currentTimeMillis(),
                    text
                ),
                new RoomWithSendBackCheckFilter(room, connection)
            );
        }

    }
}
