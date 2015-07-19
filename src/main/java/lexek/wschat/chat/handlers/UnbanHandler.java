package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.chat.Chatter;

public class UnbanHandler extends AbstractModerationHandler {
    public UnbanHandler(RoomManager roomManager) {
        super(MessageType.UNBAN, roomManager, true, "UNBAN_DENIED");
    }

    @Override
    protected boolean performOperation(Room room, Chatter mod, Chatter user) {
        return room.unbanChatter(user, mod);
    }

    @Override
    protected void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter) {
        connection.send(Message.infoMessage("OK"));
    }
}
