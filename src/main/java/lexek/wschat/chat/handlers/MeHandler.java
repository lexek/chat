package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MeHandler extends AbstractMsgHandler {
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;

    public MeHandler(MessageBroadcaster messageBroadcaster, AtomicLong messageId, RoomManager roomManager) {
        super(MessageType.ME, 2, true, roomManager);

        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
    }

    @Override
    protected void handle(Connection connection, Room room, Chatter chatter, List<String> args) {
        String msg = args.get(1).trim();
        if (msg.length() == 0) {
            connection.send(Message.errorMessage("EMPTY_MESSAGE"));
        } else if (!(chatter.hasRole(LocalRole.MOD) || connection.getUser().hasRole(GlobalRole.MOD)) && (msg.length() > 420)) {
            connection.send(Message.errorMessage("MESSAGE_TOO_BIG"));
        } else if (msg.length() != 0) {
            Message message = Message.meMessage(
                room.getName(),
                chatter.getUser().getName(),
                chatter.getRole(),
                chatter.getUser().getRole(),
                chatter.getUser().getColor(),
                messageId.getAndIncrement(),
                System.currentTimeMillis(),
                msg);
            messageBroadcaster.submitMessage(message, connection, room.FILTER);
        }
    }
}
