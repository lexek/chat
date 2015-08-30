package lexek.wschat.db.model.rest;

public class ProxyProviderRestModel {
    private final String name;
    private final boolean supportsAuthentication;
    private final boolean supportsOutbound;

    public ProxyProviderRestModel(String name, boolean supportsAuthentication, boolean supportsOutbound) {
        this.name = name;
        this.supportsAuthentication = supportsAuthentication;
        this.supportsOutbound = supportsOutbound;
    }

    public String getName() {
        return name;
    }

    public boolean getSupportsAuthentication() {
        return supportsAuthentication;
    }

    public boolean getSupportsOutbound() {
        return supportsOutbound;
    }
}
