package lexek.wschat.proxy;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.dao.ProxyAuthDao;
import lexek.wschat.db.model.ProxyAuth;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.social.SocialAuthService;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.security.social.SocialToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ProxyAuthService {
    private final Logger logger = LoggerFactory.getLogger(ProxyAuthService.class);
    private final Map<String, SocialAuthService> socialAuthServices;
    private final ProxyAuthDao proxyAuthDao;
    private final HashMap<Long, SocialProfile> tokenCache = new HashMap<>();

    public ProxyAuthService(Map<String, SocialAuthService> socialAuthServices, ProxyAuthDao proxyAuthDao) {
        this.socialAuthServices = socialAuthServices;
        this.proxyAuthDao = proxyAuthDao;
    }

    public SocialAuthService getAuthService(String name) {
        return socialAuthServices.get(name);
    }

    public Collection<SocialAuthService> getServices() {
        return socialAuthServices.values();
    }

    public synchronized String getToken(Long authId) {
        SocialProfile cachedProfile = tokenCache.get(authId);
        if (cachedProfile != null) {
            SocialToken cachedToken = cachedProfile.getToken();
            SocialAuthService socialAuthService = getAuthService(cachedToken.getService());
            Long expires = cachedToken.getExpires();
            if (expires != null) {
                if (expires < System.currentTimeMillis()) {
                    try {
                        SocialToken refreshedToken = refresh(socialAuthService, cachedToken, authId);
                        tokenCache.put(authId, new SocialProfile(
                            cachedProfile.getId(),
                            cachedProfile.getService(),
                            cachedProfile.getName(),
                            cachedProfile.getEmail(),
                            refreshedToken
                        ));
                        return refreshedToken.getToken();
                    } catch (IOException e) {
                        //todo: ?
                        throw new RuntimeException(e);
                    }
                } else {
                    return cachedToken.getToken();
                }
            } else {
                return cachedToken.getToken();
            }
        }
        return null;
    }

    public SocialProfile getProfile(long authId) {
        return tokenCache.get(authId);
    }

    public ProxyAuth getAuth(Long authId) {
        return proxyAuthDao.get(authId);
    }

    public synchronized void registerToken(UserDto owner, SocialProfile socialProfile) {
        String saveToken = socialProfile.getToken().getToken();
        if (socialProfile.getToken().getRefreshToken() != null) {
            saveToken = socialProfile.getToken().getRefreshToken();
        }
        ProxyAuth auth = proxyAuthDao.createOrUpdate(new ProxyAuth(
            null,
            socialProfile.getService(),
            socialProfile.getId(),
            socialProfile.getName(),
            owner,
            saveToken
        ));
        tokenCache.put(auth.getId(), socialProfile);
    }

    public synchronized void deleteAuth(long authId, UserDto owner) {
        if (owner.hasRole(GlobalRole.SUPERADMIN)) {
            proxyAuthDao.delete(authId);
        } else {
            proxyAuthDao.delete(authId, owner.getId());
        }
        tokenCache.remove(authId);
    }

    public List<ProxyAuth> getAllCredentials(UserDto owner) {
        if (owner.hasRole(GlobalRole.SUPERADMIN)) {
            return proxyAuthDao.getAll();
        } else {
            return proxyAuthDao.getAll(owner);
        }
    }

    public List<ProxyAuth> getAvailableCredentials(UserDto owner, Set<String> services) {
        if (owner.hasRole(GlobalRole.SUPERADMIN)) {
            return proxyAuthDao.getAll(services);
        } else {
            return proxyAuthDao.getAll(owner, services);
        }
    }

    public synchronized void loadTokens() {
        for (ProxyAuth proxyAuth : proxyAuthDao.getAll()) {
            String serviceName = proxyAuth.getService();
            SocialAuthService service = getAuthService(serviceName);
            if (service == null) {
                logger.error("no service with name {}", serviceName);
                return;
            }
            Long id = proxyAuth.getId();
            if (service.needsRefreshing()) {
                SocialToken tempToken = new SocialToken(
                    serviceName,
                    null,
                    0L,
                    proxyAuth.getKey()
                );
                try {
                    tokenCache.put(id, new SocialProfile(
                        proxyAuth.getExternalId(),
                        serviceName,
                        proxyAuth.getExternalName(),
                        null,
                        refresh(service, tempToken, id)
                    ));
                } catch (IOException e) {
                    logger.error("couldn't refresh token {}/{}", service, proxyAuth.getExternalName());
                    tokenCache.put(id, new SocialProfile(
                        proxyAuth.getExternalId(),
                        serviceName,
                        proxyAuth.getExternalName(),
                        null,
                        tempToken
                    ));
                }
            } else {
                tokenCache.put(
                    id,
                    new SocialProfile(
                        proxyAuth.getExternalId(),
                        serviceName,
                        proxyAuth.getExternalName(),
                        null,
                        new SocialToken(
                            serviceName,
                            proxyAuth.getKey(),
                            null,
                            null
                        )
                    )
                );
            }
        }
    }

    private SocialToken refresh(SocialAuthService service, SocialToken token, long authId) throws IOException {
        SocialToken refreshedToken = service.refresh(token);
        if (!refreshedToken.getRefreshToken().equals(token.getRefreshToken())) {
            proxyAuthDao.updateToken(authId, refreshedToken.getRefreshToken());
        }
        return refreshedToken;
    }
}
