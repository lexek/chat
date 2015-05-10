package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;

import java.util.concurrent.TimeUnit;

public class TimeOutHandler extends AbstractModerationHandler {
    private final MessageBroadcaster messageBroadcaster;

    public TimeOutHandler(MessageBroadcaster messageBroadcaster, RoomManager roomManager) {
        super(MessageType.TIMEOUT, roomManager, true, "TIMEOUT_DENIED");
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    protected boolean performOperation(Room room, Chatter userChatter) {
        return room.timeoutChatter(userChatter, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
    }

    @Override
    protected void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter) {
        Message message = Message.moderationMessage(
                MessageType.TIMEOUT,
                room.getName(),
                modChatter.getUser().getName(),
                userChatter.getUser().getName()
        );
        messageBroadcaster.submitMessage(message, connection, room.FILTER);
    }
}
