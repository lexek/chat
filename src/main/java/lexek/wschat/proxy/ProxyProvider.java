package lexek.wschat.proxy;

import lexek.wschat.chat.Room;

import java.util.EnumSet;

public abstract class ProxyProvider {
    private final String name;
    private final boolean supportsAuthentication;
    private final boolean supportsOutbound;
    private final EnumSet<ModerationOperation> supportedOperations;

    public ProxyProvider(
        String name,
        boolean supportsAuthentication,
        boolean supportsOutbound,
        EnumSet<ModerationOperation> supportedOperations
    ) {
        this.name = name;
        this.supportsAuthentication = supportsAuthentication;
        this.supportsOutbound = supportsOutbound;
        this.supportedOperations = supportedOperations;
    }

    public String getName() {
        return name;
    }

    public boolean isSupportsAuthentication() {
        return supportsAuthentication;
    }

    public boolean isSupportsOutbound() {
        return supportsOutbound;
    }

    public EnumSet<ModerationOperation> getSupportedOperations() {
        return supportedOperations;
    }

    public boolean supports(ModerationOperation operation) {
        return supportedOperations.contains(operation);
    }

    public abstract Proxy newProxy(long id, Room room, String remoteRoom, String name, String key, boolean outbound);

    public abstract boolean validateCredentials(String name, String token);
}
