package lexek.wschat.db;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JdbcDataBaseConfiguration {
    private final String uri;
    private final String username;
    private final String password;

    public JdbcDataBaseConfiguration(@JsonProperty("uri") String uri,
                                     @JsonProperty("username") String username,
                                     @JsonProperty("password") String password) {
        this.uri = uri;
        this.username = username;
        this.password = password;
    }

    public String getUri() {
        return uri;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
