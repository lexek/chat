package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;

public class ClearUserHandler extends AbstractModerationHandler {
    private final MessageBroadcaster messageBroadcaster;

    public ClearUserHandler(MessageBroadcaster messageBroadcaster, RoomManager roomManager) {
        super(MessageType.CLEAR, roomManager, false, "CLEAR_DENIED");
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    protected boolean performOperation(Room room, Chatter mod, Chatter user) {
        return true;
    }

    @Override
    protected void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter) {
        Message message = Message.moderationMessage(
            MessageType.CLEAR,
            room.getName(),
            modChatter.getUser().getName(),
            userChatter.getUser().getName()
        );
        messageBroadcaster.submitMessage(message, connection, room.FILTER);
    }
}
