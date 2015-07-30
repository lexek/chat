package lexek.wschat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lexek.wschat.db.JdbcDataBaseConfiguration;
import lexek.wschat.frontend.http.HttpConfiguration;
import lexek.wschat.proxy.ProxyConfiguration;

public class Configuration {
    private final HttpConfiguration http;
    private final CoreConfiguration core;
    private final JdbcDataBaseConfiguration db;
    private final EmailConfiguration email;
    private final ProxyConfiguration[] proxy;

    Configuration(@JsonProperty("proxy") ProxyConfiguration[] proxyConfigurations,
                  @JsonProperty("http") HttpConfiguration http,
                  @JsonProperty("core") CoreConfiguration core,
                  @JsonProperty("db") JdbcDataBaseConfiguration db,
                  @JsonProperty("email") EmailConfiguration email) {
        this.proxy = proxyConfigurations;
        this.http = http;
        this.core = core;
        this.db = db;
        this.email = email;
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

    public ProxyConfiguration[] getProxy() {
        return proxy;
    }

    public EmailConfiguration getEmail() {
        return email;
    }
}
