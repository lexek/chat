package lexek.wschat.db.model.form;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AnnouncementForm {
    @NotNull
    @Size(min = 5, max = 1024)
    private final String text;
    private final boolean onlyBroadcast;

    public AnnouncementForm(@JsonProperty("text") String text,
                            @JsonProperty("onlyBroadcast") boolean onlyBroadcast) {
        this.text = text;
        this.onlyBroadcast = onlyBroadcast;
    }

    public String getText() {
        return text;
    }

    public boolean isOnlyBroadcast() {
        return onlyBroadcast;
    }
}
