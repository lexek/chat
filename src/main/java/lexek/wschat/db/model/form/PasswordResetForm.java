package lexek.wschat.db.model.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotBlank;

public class PasswordResetForm {
    @NotBlank(message = "CAPTCHA_REQUIRED")
    private final String captcha;
    @NotBlank(message = "NAME_REQUIRED")
    private final String name;

    public PasswordResetForm(
        @JsonProperty("captcha") String captcha,
        @JsonProperty("name") String name
    ) {
        this.captcha = captcha;
        this.name = name;
    }

    public String getCaptcha() {
        return captcha;
    }

    public String getName() {
        return name;
    }
}
