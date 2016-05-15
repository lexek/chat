package lexek.wschat.security.social;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.chat.e.AccessDeniedException;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.SecureTokenGenerator;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SocialAuthService {
    private final Cache<String, TemporarySession> temporarySessions = CacheBuilder
        .newBuilder()
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .build();
    private final AuthenticationManager authenticationManager;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Map<String, SocialAuthProvider> providers = new ConcurrentHashMapV8<>();

    public SocialAuthService(AuthenticationManager authenticationManager, SecureTokenGenerator secureTokenGenerator) {
        this.authenticationManager = authenticationManager;
        this.secureTokenGenerator = secureTokenGenerator;
    }

    public void registerProvider(SocialAuthProvider provider) {
        providers.put(provider.getName(), provider);
    }

    public SocialAuthProvider getAuthService(String name) {
        return providers.get(name);
    }

    public SessionResult getSession(SocialProfile profile, String ip) {
        UserAuthDto userAuth = authenticationManager.getOrCreateUserAuth(profile, false);
        if (userAuth != null) {
            SessionDto session = authenticationManager.createSession(userAuth, ip);
            return new SessionResult(
                SocialAuthResultType.SESSION,
                session.getSessionId()
            );
        } else {
            TemporarySession temporarySession = new TemporarySession(profile, ip);
            String sessionId = secureTokenGenerator.generateShortSessionId();
            temporarySessions.put(sessionId, temporarySession);
            return new SessionResult(
                SocialAuthResultType.TEMP_SESSION,
                sessionId
            );
        }
    }

    public void expireTemporarySession(String id) {
        temporarySessions.invalidate(id);
    }

    public SocialProfile getTempSession(String id, String ip) {
        if (id != null) {
            TemporarySession session = temporarySessions.getIfPresent(id);
            if (session != null) {
                if (!session.getIp().equals(ip)) {
                    throw new InvalidInputException("temp_sid", "unable to verify session");
                }
                return session.getProfile();
            }
        }
        throw new AccessDeniedException("Temporary session is invalid or expired");
    }

    private class TemporarySession {
        private final SocialProfile profile;
        private final String ip;

        private TemporarySession(SocialProfile profile, String ip) {
            this.profile = profile;
            this.ip = ip;
        }

        public SocialProfile getProfile() {
            return profile;
        }

        public String getIp() {
            return ip;
        }
    }
}
