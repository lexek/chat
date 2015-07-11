package lexek.wschat.services.poll;

import com.google.common.collect.ImmutableList;

public class Poll {
    private final long id;
    private final String question;
    private final ImmutableList<PollOption> options;

    public Poll(long id, String question, ImmutableList<PollOption> options) {
        this.id = id;
        this.question = question;
        this.options = options;
    }

    public long getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public ImmutableList<PollOption> getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return "Poll{" +
            "id=" + id +
            ", question='" + question + '\'' +
            ", options=" + options +
            '}';
    }
}
