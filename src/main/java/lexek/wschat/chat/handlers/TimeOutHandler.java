package lexek.wschat.chat.handlers;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.services.ChatterService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@Service
public class TimeOutHandler extends AbstractModerationHandler {
    private final ChatterService chatterService;

    @Inject
    public TimeOutHandler(ChatterService chatterService) {
        super(MessageType.TIMEOUT, true, "TIMEOUT_DENIED");
        this.chatterService = chatterService;
    }

    @Override
    protected boolean performOperation(Room room, Chatter mod, Chatter user) {
        return chatterService.timeoutChatter(room, user, mod, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
    }

    @Override
    protected void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter) {
    }
}
