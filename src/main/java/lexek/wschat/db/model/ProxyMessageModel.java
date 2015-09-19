package lexek.wschat.db.model;

public class ProxyMessageModel {
    private final String providerName;
    private final String remoteRoom;
    private final boolean outboundEnabled;
    private final boolean moderationEnabled;

    public ProxyMessageModel(String providerName, String remoteRoom, boolean outboundEnabled, boolean moderationEnabled) {
        this.providerName = providerName;
        this.remoteRoom = remoteRoom;
        this.outboundEnabled = outboundEnabled;
        this.moderationEnabled = moderationEnabled;
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

    public boolean isModerationEnabled() {
        return moderationEnabled;
    }
}
