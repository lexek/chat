package lexek.wschat.db.model.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Email;

import javax.validation.constraints.NotNull;

public class EmailForm {
    @Email
    @NotNull
    private final String email;

    public EmailForm(@JsonProperty("email") String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
