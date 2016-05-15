package lexek.wschat.security.social;

import io.netty.util.internal.chmv8.ConcurrentHashMapV8;

import java.util.Map;

public class SocialAuthService {
    private final Map<String, SocialAuthProvider> providers = new ConcurrentHashMapV8<>();

    public void registerProvider(SocialAuthProvider provider) {
        providers.put(provider.getName(), provider);
    }

    public SocialAuthProvider getAuthService(String name) {
        return providers.get(name);
    }
}
