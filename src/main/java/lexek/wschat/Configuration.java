package lexek.wschat;

import lexek.wschat.db.JdbcDataBaseConfiguration;
import lexek.wschat.frontend.http.HttpConfiguration;
import lexek.wschat.proxy.ProxyConfiguration;

public class Configuration {
    private final HttpConfiguration http;
    private final CoreConfiguration core;
    private final JdbcDataBaseConfiguration db;
    private final EmailConfiguration email;
    private final ProxyConfiguration[] proxy;

    Configuration(ProxyConfiguration[] proxyConfigurations, HttpConfiguration http, CoreConfiguration core, JdbcDataBaseConfiguration db, EmailConfiguration email) {
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
