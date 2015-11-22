package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.filters.UserInRoomFilter;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;
import lexek.wschat.services.poll.PollService;


public class VoteHandler extends AbstractRoomMessageHandler {
    private final PollService pollService;
    private final MessageBroadcaster messageBroadcaster;

    public VoteHandler(PollService pollService, MessageBroadcaster messageBroadcaster) {
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
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    public void handle(Connection connection, User user, Room room, Chatter chatter, Message message) {
        int option = message.get(MessageProperty.POLL_OPTION);
        if (option >= 0) {
            pollService.vote(room, user, option);
            messageBroadcaster.submitMessage(
                Message.pollVotedMessage(room.getName()),
                new UserInRoomFilter(user, room)
            );
        } else {
            connection.send(Message.errorMessage("BAD_ARG"));
        }
    }
}
