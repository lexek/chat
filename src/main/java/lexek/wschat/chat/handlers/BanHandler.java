package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.services.ChatterService;

public class BanHandler extends AbstractModerationHandler {
    private final MessageBroadcaster messageBroadcaster;
    private final ChatterService chatterService;

    public BanHandler(MessageBroadcaster messageBroadcaster, ChatterService chatterService) {
        super(MessageType.BAN, true, "BAN_DENIED");
        this.messageBroadcaster = messageBroadcaster;
        this.chatterService = chatterService;
    }

    @Override
    protected boolean performOperation(Room room, Chatter mod, Chatter user) {
        return chatterService.banChatter(room, user, mod);
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
