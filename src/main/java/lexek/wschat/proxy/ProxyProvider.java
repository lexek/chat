package lexek.wschat.proxy;

import lexek.wschat.chat.Room;
import org.jvnet.hk2.annotations.Contract;

import java.util.EnumSet;
import java.util.Set;

@Contract
public abstract class ProxyProvider {
    private final String name;
    private final boolean supportsAuth;
    private final boolean supportsOutbound;
    private final boolean requiresAuth;

    private final Set<String> supportedAuthServices;
    private final EnumSet<ModerationOperation> supportedOperations;

    public ProxyProvider(
        String name,
        boolean supportsAuth,
        boolean supportsOutbound,
        boolean requiresAuth,
        Set<String> supportedAuthServices,
        EnumSet<ModerationOperation> supportedOperations
    ) {
        this.name = name;
        this.supportsAuth = supportsAuth;
        this.supportsOutbound = supportsOutbound;
        this.requiresAuth = requiresAuth;
        this.supportedAuthServices = supportedAuthServices;
        this.supportedOperations = supportedOperations;
    }

    public String getName() {
        return name;
    }

    public boolean isSupportsAuth() {
        return supportsAuth;
    }

    public boolean isSupportsOutbound() {
        return supportsOutbound;
    }

    public boolean supportsModerationOperation(ModerationOperation operation) {
        return supportedOperations.contains(operation);
    }

    public boolean supportsAuthService(String service) {
        return supportedAuthServices.contains(service);
    }

    public Set<String> getSupportedAuthServices() {
        return supportedAuthServices;
    }

    public boolean requiresAuth() {
        return requiresAuth;
    }

    public abstract Proxy newProxy(long id, Room room, String remoteRoom, Long proxyAuthId, boolean outbound);

    public abstract boolean validateRemoteRoom(String remoteRoom);
}
