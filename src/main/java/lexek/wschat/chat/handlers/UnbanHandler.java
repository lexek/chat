package lexek.wschat.chat.handlers;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.services.ChatterService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class UnbanHandler extends AbstractModerationHandler {
    private final ChatterService chatterService;

    @Inject
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
