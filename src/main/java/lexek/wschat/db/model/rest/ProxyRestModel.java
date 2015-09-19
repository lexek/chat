package lexek.wschat.db.model.rest;

import lexek.wschat.proxy.ProxyState;

public class ProxyRestModel {
    private final long id;
    private final String providerName;
    private final String remoteRoom;
    private final String lastError;
    private final ProxyState state;
    private final boolean outboundEnabled;
    private final boolean moderationEnabled;

    public ProxyRestModel(long id, String providerName, String remoteRoom, String lastError, ProxyState state, boolean outboundEnabled, boolean moderationEnabled) {
        this.id = id;
        this.providerName = providerName;
        this.remoteRoom = remoteRoom;
        this.lastError = lastError;
        this.state = state;
        this.outboundEnabled = outboundEnabled;
        this.moderationEnabled = moderationEnabled;
    }

    public long getId() {
        return id;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getRemoteRoom() {
        return remoteRoom;
    }

    public boolean isOutboundEnabled() {
        return outboundEnabled;
    }

    public ProxyState getState() {
        return state;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isModerationEnabled() {
        return moderationEnabled;
    }
}
