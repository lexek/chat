package lexek.wschat.chat.listeners;

import lexek.wschat.chat.*;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.services.poll.PollService;
import lexek.wschat.services.poll.PollState;

public class SendPollOnEventListener implements EventListener {
    private final PollService pollService;

    public SendPollOnEventListener(PollService pollService) {
        this.pollService = pollService;
    }

    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        PollState activePoll = pollService.getActivePoll(room);
        if (activePoll != null) {
            connection.send(Message.pollMessage(MessageType.POLL, room.getName(), activePoll));
            if (activePoll.getVoted().contains(connection.getUser().getId())) {
                connection.send(Message.pollVotedMessage(room.getName()));
            }
        }
    }
}