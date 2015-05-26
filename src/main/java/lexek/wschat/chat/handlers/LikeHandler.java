package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.db.model.Chatter;

import java.util.List;

public class LikeHandler extends AbstractMsgHandler {
    private final MessageBroadcaster messageBroadcaster;

    public LikeHandler(MessageBroadcaster messageBroadcaster, RoomManager roomManager) {
        super(MessageType.LIKE, 2, true, roomManager);
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    protected void handle(Connection connection, Room room, Chatter chatter, List<String> args) {
        Long id = Long.parseLong(args.get(1));
        Message message = Message.likeMessage(
                room.getName(),
                connection.getUser().getName(),
                id);
        messageBroadcaster.submitMessage(message, connection, room.FILTER);
    }
}
