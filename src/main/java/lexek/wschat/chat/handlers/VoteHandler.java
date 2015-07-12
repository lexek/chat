package lexek.wschat.chat.handlers;

import com.google.common.primitives.Ints;
import lexek.wschat.chat.*;
import lexek.wschat.services.poll.PollService;

import java.util.List;

public class VoteHandler extends AbstractMessageHandler {
    private final RoomManager roomManager;
    private final PollService pollService;

    public VoteHandler(RoomManager roomManager, PollService pollService) {
        super(MessageType.POLL_VOTE, GlobalRole.USER, 2, true, false);
        this.roomManager = roomManager;
        this.pollService = pollService;
    }

    @Override
    public void handle(List<String> args, Connection connection) {
        Room room = roomManager.getRoomInstance(args.get(0));
        User user = connection.getUser();
        if (room != null) {
            if (room.contains(connection)) {
                Integer option = Ints.tryParse(args.get(1));
                if (option != null && option >= 0) {
                    pollService.vote(room, user, option);
                    connection.send(Message.pollVotedMessage(room.getName()));
                } else {
                    connection.send(Message.errorMessage("BAD_ARG"));
                }
            } else {
                connection.send(Message.errorMessage("NOT_JOINED"));
            }
        } else {
            connection.send(Message.errorMessage("UNKNOWN_ROOM"));
        }
    }
}
