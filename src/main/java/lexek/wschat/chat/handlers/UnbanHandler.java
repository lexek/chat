package lexek.wschat.chat.handlers;

import lexek.wschat.chat.*;
import lexek.wschat.chat.filters.UserInRoomFilter;
import lexek.wschat.services.ChatterService;

public class UnbanHandler extends AbstractModerationHandler {
    private final ChatterService chatterService;
    private final MessageBroadcaster messageBroadcaster;

    public UnbanHandler(ChatterService chatterService, MessageBroadcaster messageBroadcaster) {
        super(MessageType.UNBAN, true, "UNBAN_DENIED");
        this.chatterService = chatterService;
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    protected boolean performOperation(Room room, Chatter mod, Chatter user) {
        return chatterService.unbanChatter(room, user, mod);
    }

    @Override
    protected void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter) {
        connection.send(Message.infoMessage("OK"));
        messageBroadcaster.submitMessage(
            Message.infoMessage("You have been unbanned", room.getName()),
            new UserInRoomFilter(userChatter.getUser(), room)
        );
    }
}
