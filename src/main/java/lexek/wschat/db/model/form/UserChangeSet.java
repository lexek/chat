package lexek.wschat.db.model.form;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lexek.wschat.chat.model.GlobalRole;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserChangeSet {
    private final Boolean renameAvailable;
    private final Boolean banned;
    private final GlobalRole role;
    private final String name;

    public UserChangeSet(@JsonProperty("rename") Boolean renameAvailable,
                         @JsonProperty("banned") Boolean banned,
                         @JsonProperty("role") GlobalRole role,
                         @JsonProperty("name") String name) {
        this.renameAvailable = renameAvailable;
        this.banned = banned;
        this.role = role;
        this.name = name;
    }

    public Boolean getRenameAvailable() {
        return renameAvailable;
    }

    public Boolean getBanned() {
        return banned;
    }

    public GlobalRole getRole() {
        return role;
    }

    public String getName() {
        return name;
    }
}
