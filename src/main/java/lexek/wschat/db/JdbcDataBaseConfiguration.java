package lexek.wschat.db;

public class JdbcDataBaseConfiguration {
    private final String uri;
    private final String username;
    private final String password;

    public JdbcDataBaseConfiguration(String uri, String username, String password) {
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
