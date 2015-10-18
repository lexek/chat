package lexek.wschat.db.model.form;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class RoomForm {
    @NotNull
    @Pattern(regexp = "#[a-z]{3,10}", message = "name must start with hash and length must be betweeen 3 and 10")
    private final String name;
    @NotNull
    @Size(max = 50)
    private final String topic;

    public RoomForm(@JsonProperty("name") String name,
                    @JsonProperty("topic") String topic) {
        this.name = name;
        this.topic = topic;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }
}
