package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;

public class BanHandler extends AbstractModerationHandler {
    private final MessageBroadcaster messageBroadcaster;

    public BanHandler(MessageBroadcaster messageBroadcaster, RoomManager roomManager) {
        super(MessageType.BAN, roomManager, true, "BAN_DENIED");
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    protected boolean performOperation(Room room, Chatter mod, Chatter user) {
        return room.banChatter(user, mod);
    }

    @Override
    protected void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter) {
        Message message = Message.moderationMessage(
                MessageType.BAN,
                room.getName(),
                modChatter.getUser().getName(),
                userChatter.getUser().getName()
        );
        messageBroadcaster.submitMessage(message, connection, room.FILTER);
    }
}
