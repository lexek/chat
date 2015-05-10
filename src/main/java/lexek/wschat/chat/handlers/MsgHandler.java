package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MsgHandler extends AbstractMsgHandler {
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;

    public MsgHandler(AtomicLong messageId, MessageBroadcaster messageBroadcaster, RoomManager roomManager) {
        super(MessageType.MSG, 2, true, roomManager);
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    protected void handle(Connection connection, Room room, Chatter chatter, List<String> args) {
        if (room != null && room.contains(connection)) {
            String msg = args.get(1).trim();
            if (msg.length() == 0) {
                connection.send(Message.errorMessage("EMPTY_MESSAGE"));
            } else if ((chatter.hasRole(LocalRole.MOD) || connection.getUser().hasRole(GlobalRole.MOD)) && (msg.length() > 420)) {
                connection.send(Message.errorMessage("MESSAGE_TOO_BIG"));
            } else {
                Message message = Message.msgMessage(
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
}
