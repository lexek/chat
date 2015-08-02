package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;
import lexek.wschat.services.poll.PollService;


public class VoteHandler extends AbstractRoomMessageHandler {
    private final PollService pollService;

    public VoteHandler(PollService pollService) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM,
                MessageProperty.POLL_OPTION
            ),
            MessageType.POLL_VOTE,
            LocalRole.USER,
            true
        );
        this.pollService = pollService;
    }

    @Override
    public void handle(Connection connection, User user, Room room, Chatter chatter, Message message) {
        int option = message.get(MessageProperty.POLL_OPTION);
        if (option >= 0) {
            pollService.vote(room, user, option);
            connection.send(Message.pollVotedMessage(room.getName()));
        } else {
            connection.send(Message.errorMessage("BAD_ARG"));
        }
    }
}
