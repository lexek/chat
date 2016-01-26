package lexek.wschat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lexek.wschat.db.JdbcDataBaseConfiguration;
import lexek.wschat.frontend.http.HttpConfiguration;
import lexek.wschat.proxy.twitter.TwitterCredentials;

public class Configuration {
    private final HttpConfiguration http;
    private final CoreConfiguration core;
    private final JdbcDataBaseConfiguration db;
    private final EmailConfiguration email;
    private final TwitterCredentials twitter;

    Configuration(
        @JsonProperty("http") HttpConfiguration http,
        @JsonProperty("core") CoreConfiguration core,
        @JsonProperty("db") JdbcDataBaseConfiguration db,
        @JsonProperty("email") EmailConfiguration email,
        @JsonProperty("twitter") TwitterCredentials twitter
    ) {
        this.http = http;
        this.core = core;
        this.db = db;
        this.email = email;
        this.twitter = twitter;
    }

    public HttpConfiguration getHttp() {
        return http;
    }

    public CoreConfiguration getCore() {
        return core;
    }

    public JdbcDataBaseConfiguration getDb() {
        return db;
    }

    public EmailConfiguration getEmail() {
        return email;
    }

    public TwitterCredentials getTwitter() {
        return twitter;
    }
}
