package lexek.wschat.db.model.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Size;

public class PasswordForm {
    @NotEmpty
    @Size(min = 6, max = 30)
    private final String password;
    @Size(min = 6, max = 30)
    private final String oldPassword;

    public PasswordForm(
        @JsonProperty("password") String password,
        @JsonProperty("oldPassword") String oldPassword
    ) {
        this.password = password;
        this.oldPassword = oldPassword;
    }

    public String getPassword() {
        return password;
    }

    public String getOldPassword() {
        return oldPassword;
    }
}
