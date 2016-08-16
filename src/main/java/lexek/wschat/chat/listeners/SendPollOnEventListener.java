package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.ChatEventType;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.services.poll.PollService;
import lexek.wschat.services.poll.PollState;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class SendPollOnEventListener implements EventListener {
    private final PollService pollService;

    @Inject
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

    @Override
    public int getOrder() {
        return 5;
    }

    @Override
    public ChatEventType getEventType() {
        return ChatEventType.JOIN;
    }
}
