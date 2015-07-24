package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;

import java.util.List;

public class ClearHandler extends AbstractMsgHandler {
    private final MessageBroadcaster messageBroadcaster;

    public ClearHandler(MessageBroadcaster messageBroadcaster, RoomManager roomManager) {
        super(MessageType.CLEAR, 1, false, roomManager);
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    protected void handle(Connection connection, Room room, Chatter modChatter, List<String> args) {
        User mod = connection.getUser();
        if (modChatter.hasRole(LocalRole.MOD) || mod.hasRole(GlobalRole.MOD)) {
            messageBroadcaster.submitMessage(Message.clearMessage(room.getName()), connection, room.FILTER);
        } else {
            connection.send(Message.errorMessage("NOT_AUTHORIZED"));
        }
    }
}
