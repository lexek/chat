package lexek.wschat.db.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordModel {
    private final String password;

    public PasswordModel(@JsonProperty("password") String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
