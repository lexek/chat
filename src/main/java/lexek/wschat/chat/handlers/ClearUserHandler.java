package lexek.wschat.chat.handlers;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageType;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class ClearUserHandler extends AbstractModerationHandler {
    private final MessageBroadcaster messageBroadcaster;

    @Inject
    public ClearUserHandler(MessageBroadcaster messageBroadcaster) {
        super(MessageType.CLEAR, false, "CLEAR_DENIED");
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
        messageBroadcaster.submitMessage(message, room.FILTER);
    }
}
