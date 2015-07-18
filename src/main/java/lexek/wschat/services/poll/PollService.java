package lexek.wschat.services.poll;

import com.google.common.collect.ImmutableList;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.chat.*;
import lexek.wschat.db.dao.PollDao;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.JournalService;

import java.util.List;
import java.util.Map;

public class PollService {
    private final PollDao pollDao;
    private final JournalService journalService;
    private final MessageBroadcaster messageBroadcaster;
    private final Map<Room, PollState> activePolls = new ConcurrentHashMapV8<>();

    public PollService(PollDao pollDao, MessageBroadcaster messageBroadcaster, RoomManager roomManager, JournalService journalService) {
        this.pollDao = pollDao;
        this.messageBroadcaster = messageBroadcaster;
        this.journalService = journalService;
        for (Map.Entry<Long, PollState> entry : pollDao.getAllPolls().entrySet()) {
            activePolls.put(roomManager.getRoomInstance(entry.getKey()), entry.getValue());
        }
    }

    public PollState createPoll(Room room, UserDto admin, String question, List<String> options) {
        if (activePolls.containsKey(room)) {
            return null;
        }
        ImmutableList.Builder<PollOption> listBuilder = ImmutableList.builder();
        int i = 0;
        for (String option : options) {
            listBuilder.add(new PollOption(i++, option));
        }
        Poll poll = pollDao.add(question, listBuilder.build(), room.getId());
        if (poll != null) {
            PollState pollState = new PollState(poll);
            this.activePolls.put(room, pollState);
            this.messageBroadcaster.submitMessage(Message.pollMessage(MessageType.POLL, room.getName(), pollState),
                Connection.STUB_CONNECTION, room.FILTER);
            this.journalService.newPoll(admin, room, poll);
            return pollState;
        } else {
            return null;
        }
    }

    public boolean vote(Room room, User user, int option) {
        boolean result = false;
        PollState pollState = activePolls.get(room);
        if (pollState != null && option < pollState.getPoll().getOptions().size()) {
            if (!pollState.getVoted().contains(user.getId())) {
                result = pollDao.vote(pollState.getPoll().getId(), user.getId(), option);
            }
            if (result) {
                pollState.addVote(pollState.getPoll().getOptions().get(option), user.getId());
                this.messageBroadcaster.submitMessage(
                    Message.pollMessage(MessageType.POLL_UPDATE, room.getName(), pollState),
                    Connection.STUB_CONNECTION, room.FILTER);
            }
        }
        return result;
    }

    public void closePoll(Room room, UserDto admin) {
        PollState pollState = activePolls.remove(room);
        if (pollState != null) {
            pollDao.closePoll(pollState.getPoll().getId());
            this.messageBroadcaster.submitMessage(Message.pollMessage(MessageType.POLL_END, room.getName(), pollState),
                Connection.STUB_CONNECTION, room.FILTER);
            this.journalService.closedPoll(admin, room, pollState.getPoll());
        }
    }

    public PollState getActivePoll(Room room) {
        return this.activePolls.get(room);
    }

    public DataPage<PollState> getOldPolls(Room room, int page) {
        return pollDao.getOldPolls(room.getId(), page);
    }
}
