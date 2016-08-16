package lexek.wschat.chat.handlers;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.services.ChatterService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class BanHandler extends AbstractModerationHandler {
    private final ChatterService chatterService;

    @Inject
    public BanHandler(ChatterService chatterService) {
        super(MessageType.BAN, true, "BAN_DENIED");
        this.chatterService = chatterService;
    }

    @Override
    protected boolean performOperation(Room room, Chatter mod, Chatter user) {
        return chatterService.banChatter(room, user, mod);
    }

    @Override
    protected void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter) {
    }
}
