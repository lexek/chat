package lexek.wschat.db.model.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class UsernameForm {
    @NotNull
    @NotEmpty
    private final String name;

    public UsernameForm(@JsonProperty("name") String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
