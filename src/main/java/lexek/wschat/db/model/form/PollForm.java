package lexek.wschat.db.model.form;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class PollForm {
    @Size(min = 4, max = 50)
    @NotNull
    private final String question;
    @Size(min = 2, max = 5)
    @NotNull
    private final List<String> options;

    public PollForm(@JsonProperty("question") String question,
                    @JsonProperty("options") List<String> options) {
        this.question = question;
        this.options = options;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }
}
