package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;

public class UnbanHandler extends AbstractModerationHandler {
    public UnbanHandler(RoomManager roomManager) {
        super(MessageType.UNBAN, roomManager, true, "UNBAN_DENIED");
    }

    @Override
    protected boolean performOperation(Room room, Chatter userChatter) {
        return room.unbanChatter(userChatter);
    }

    @Override
    protected void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter) {
        connection.send(Message.infoMessage("OK"));
    }
}
