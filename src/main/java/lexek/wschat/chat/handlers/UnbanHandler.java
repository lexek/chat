package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.services.ChatterService;

public class UnbanHandler extends AbstractModerationHandler {
    private final ChatterService chatterService;

    public UnbanHandler(ChatterService chatterService) {
        super(MessageType.UNBAN, true, "UNBAN_DENIED");
        this.chatterService = chatterService;
    }

    @Override
    protected boolean performOperation(Room room, Chatter mod, Chatter user) {
        return chatterService.unbanChatter(room, user, mod);
    }

    @Override
    protected void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter) {
        connection.send(Message.infoMessage("OK"));
    }
}
