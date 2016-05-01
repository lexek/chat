package lexek.wschat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lexek.wschat.db.JdbcDataBaseConfiguration;
import lexek.wschat.frontend.http.HttpConfiguration;
import lexek.wschat.proxy.twitter.TwitterCredentials;

import java.util.Map;

public class Configuration {
    private final HttpConfiguration http;
    private final CoreConfiguration core;
    private final JdbcDataBaseConfiguration db;
    private final EmailConfiguration email;
    private final TwitterCredentials twitter;
    private final Map<String, ProxyAuthCredentials> proxy;

    Configuration(
        @JsonProperty("http") HttpConfiguration http,
        @JsonProperty("core") CoreConfiguration core,
        @JsonProperty("db") JdbcDataBaseConfiguration db,
        @JsonProperty("email") EmailConfiguration email,
        @JsonProperty("twitter") TwitterCredentials twitter,
        @JsonProperty("proxy") Map<String, ProxyAuthCredentials> proxy
    ) {
        this.http = http;
        this.core = core;
        this.db = db;
        this.email = email;
        this.twitter = twitter;
        this.proxy = proxy;
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

    public Map<String, ProxyAuthCredentials> getProxy() {
        return proxy;
    }
}
