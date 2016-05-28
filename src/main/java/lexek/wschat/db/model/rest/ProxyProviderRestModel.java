package lexek.wschat.db.model.rest;

import lexek.wschat.db.model.ProxyAuth;

import java.util.Collection;

public class ProxyProviderRestModel {
    private final String name;
    private final boolean supportsAuth;
    private final boolean requiresAuth;
    private final boolean supportsOutbound;
    private final Collection<ProxyAuth> availableCredentials;

    public ProxyProviderRestModel(
        String name,
        boolean supportsAuth,
        boolean requiresAuth,
        boolean supportsOutbound,
        Collection<ProxyAuth> availableCredentials
    ) {
        this.name = name;
        this.supportsAuth = supportsAuth;
        this.requiresAuth = requiresAuth;
        this.supportsOutbound = supportsOutbound;
        this.availableCredentials = availableCredentials;
    }

    public String getName() {
        return name;
    }

    public boolean getSupportsAuth() {
        return supportsAuth;
    }

    public boolean getSupportsOutbound() {
        return supportsOutbound;
    }

    public boolean isRequiresAuth() {
        return requiresAuth;
    }

    public Collection<ProxyAuth> getAvailableCredentials() {
        return availableCredentials;
    }
}
