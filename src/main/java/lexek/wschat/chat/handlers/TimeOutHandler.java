package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.services.ChatterService;

import java.util.concurrent.TimeUnit;

public class TimeOutHandler extends AbstractModerationHandler {
    private final MessageBroadcaster messageBroadcaster;
    private final ChatterService chatterService;

    public TimeOutHandler(MessageBroadcaster messageBroadcaster, ChatterService chatterService) {
        super(MessageType.TIMEOUT, true, "TIMEOUT_DENIED");
        this.messageBroadcaster = messageBroadcaster;
        this.chatterService = chatterService;
    }

    @Override
    protected boolean performOperation(Room room, Chatter mod, Chatter user) {
        return chatterService.timeoutChatter(room, user, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
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
