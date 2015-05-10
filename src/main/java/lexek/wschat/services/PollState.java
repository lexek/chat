package lexek.wschat.services;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PollState {
    private final Poll poll;
    private final long[] votes;
    private final Set<Long> voted;

    public PollState(Poll poll) {
        this.poll = poll;
        this.votes = new long[poll.getOptions().size()];
        this.voted = new HashSet<>();
    }

    public PollState(Poll poll, long[] initialVotes, Set<Long> voted) {
        this.poll = poll;
        this.votes = initialVotes;
        this.voted = voted;
    }

    public Poll getPoll() {
        return poll;
    }

    public void addVote(PollOption pollOption, Long userId) {
        voted.add(userId);
        votes[pollOption.getOptionId()]++;
    }

    public long[] getVotes() {
        return votes;
    }

    public Set<Long> getVoted() {
        return voted;
    }

    @Override
    public String toString() {
        return "PollState{" +
                "poll=" + poll +
                ", votes=" + Arrays.toString(votes) +
                ", voted=" + voted +
                '}';
    }
}
