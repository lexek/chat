package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;

import java.util.concurrent.atomic.AtomicLong;

public class MeHandler extends AbstractRoomMessageHandler {
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;

    public MeHandler(MessageBroadcaster messageBroadcaster, AtomicLong messageId) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM,
                MessageProperty.TEXT
            ),
            MessageType.ME,
            LocalRole.USER,
            true
        );

        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
    }


    @Override
    public void handle(Connection connection, User user, Room room, Chatter chatter, Message message) {
        String text = message.getText().trim();
        if (text.length() == 0) {
            connection.send(Message.errorMessage("EMPTY_MESSAGE"));
        } else if (!(chatter.hasRole(LocalRole.MOD) || connection.getUser().hasRole(GlobalRole.MOD)) && (text.length() > 420)) {
            connection.send(Message.errorMessage("MESSAGE_TOO_BIG"));
        } else if (text.length() != 0) {
            messageBroadcaster.submitMessage(
                Message.meMessage(
                    room.getName(),
                    chatter.getUser().getName(),
                    chatter.getRole(),
                    chatter.getUser().getRole(),
                    chatter.getUser().getColor(),
                    messageId.getAndIncrement(),
                    System.currentTimeMillis(),
                    text
                ),
                connection,
                room.FILTER);
        }
    }
}
