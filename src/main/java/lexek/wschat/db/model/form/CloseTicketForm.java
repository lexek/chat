package lexek.wschat.db.model.form;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class CloseTicketForm {
    @NotNull
    private final String comment;

    public CloseTicketForm(@JsonProperty("comment") String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }
}
