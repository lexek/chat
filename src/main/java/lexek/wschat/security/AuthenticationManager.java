package lexek.wschat.security;

import com.google.common.io.BaseEncoding;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.httpserver.Request;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.dao.UserAuthDao;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.tx.Transactional;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.services.JournalService;
import org.apache.http.HttpHeaders;
import org.jvnet.hk2.annotations.Service;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AuthenticationManager {
    private final Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);
    private final Map<String, AtomicInteger> failedLogin = new ConcurrentHashMapV8<>();
    private final Set<UserAuthEventListener> userAuthEventListeners = new HashSet<>();
    private final SecureTokenGenerator secureTokenGenerator;
    private final JournalService journalService;
    private final UserEmailManager userEmailManager;
    private final UserAuthDao userAuthDao;
    private final SessionService sessionService;
    private final Set<String> bannedIps = new CopyOnWriteArraySet<>();

    @Inject
    public AuthenticationManager(
        SecureTokenGenerator secureTokenGenerator,
        UserAuthDao userAuthDao,
        JournalService journalService,
        UserEmailManager userEmailManager,
        SessionService sessionService
    ) {
        this.secureTokenGenerator = secureTokenGenerator;
        this.userAuthDao = userAuthDao;
        this.journalService = journalService;
        this.userEmailManager = userEmailManager;
        this.sessionService = sessionService;
    }

    public UserDto fastAuth(String name, String password, String ip) {
        AtomicInteger tries = failedLogin.get(ip);
        UserAuthDto auth = userAuthDao.getPasswordAuth(name);
        if (auth != null && auth.getAuthenticationKey() != null && validatePassword(password, auth.getAuthenticationKey())) {
            return auth.getUser();
        } else {
            if (tries == null) {
                tries = new AtomicInteger(1);
                failedLogin.put(ip, tries);
            } else {
                tries.incrementAndGet();
            }
            logger.info("failed login with password for user {} with {} tries for ip {}", name, tries, ip);
            return null;
        }
    }

    public UserDto fastAuthToken(String token) {
        return userAuthDao.getUserForToken(token);
    }

    public int failedLoginTries(String ip) {
        AtomicInteger i = failedLogin.get(ip);
        return i != null ? i.get() : 0;
    }

    public SessionDto authenticate(String username, String password, String ip) {
        UserDto user = fastAuth(username, password, ip);
        if (user != null) {
            return createSession(user, ip);
        }
        return null;
    }

    public SessionDto createSession(UserDto userAuth, String ip) {
        if (userAuth == null || ip == null) {
            throw new NullPointerException();
        }
        try {
            return sessionService.createSession(ip, userAuth);
        } catch (Exception e) {
            logger.error("exception", e);
            return null;
        }
    }

    public UserAuthDto getOrCreateUserAuth(SocialProfile profile, UserDto user) {
        try {
            UserAuthDto auth = userAuthDao.getOrCreateUserAuth(profile, user);
            if (user != null) {
                triggerAuthEvent(UserAuthEventType.CREATED, user, profile.getService());
            }
            return auth;
        } catch (Exception e) {
            return null;
        }
    }

    public UserDto checkFullAuthentication(String sid, String ip) {
        UserDto user = null;
        SessionDto session = sessionService.getSession(sid, ip);
        if (session != null) {
            user = session.getUserAuth();
        }
        return user;
    }

    public UserDto checkFullAuthentication(Request request) {
        UserDto user = null;
        if (request.hasHeader(HttpHeaders.AUTHORIZATION)) {
            String[] tmp = request.header(HttpHeaders.AUTHORIZATION).split(" ", 2);
            if (tmp.length == 2) {
                String type = tmp[0];
                String token = tmp[1];
                if (type.equals("Token")) {
                    user = fastAuthToken(token);
                } else if (type.equals("Basic")) {
                    String decodedAuth = new String(BaseEncoding.base64().decode(token));
                    if (decodedAuth.indexOf(':') != -1 && decodedAuth.length() >= 3) {
                        String[] s = decodedAuth.split(":");
                        String username = s[0];
                        String password = s[1];
                        user = fastAuth(username, password, request.ip());
                    }
                }
            }
        }
        if (user == null) {
            String sid = request.cookieValue("sid");
            if (sid != null) {
                String ip = request.ip();
                if (ip.equals("127.0.0.1")) {
                    String realIpHeader = request.header("X-REAL-IP");
                    if (realIpHeader != null) {
                        ip = realIpHeader;
                    }
                }
                user = checkFullAuthentication(sid, ip);
            }
        }
        return user;
    }

    @Transactional
    public synchronized boolean registerWithPassword(final String name, final String password, final String email) {
        String verificationCode = secureTokenGenerator.generateVerificationCode();
        UserDto user = userAuthDao.registerWithPassword(name, password, email, verificationCode);
        if (user != null) {
            userEmailManager.sendVerificationEmail(email, verificationCode, user.getId());
            userEmailManager.userCreated(user);
            journalService.userCreated(user);
        }
        return user != null;
    }

    @Transactional
    public synchronized void setPassword(UserDto user, String password, String oldPassword) {
        UserAuthDto auth = getAuthDataForUser(user, "password");
        if (auth != null && !validatePassword(oldPassword, auth.getAuthenticationKey())) {
            throw new InvalidInputException("oldPassword", "invalid");
        }
        userAuthDao.setPassword(user.getId(), BCrypt.hashpw(password, BCrypt.gensalt()));
    }

    public synchronized void setPasswordNoCheck(UserDto user, String password) {
        userAuthDao.setPassword(user.getId(), BCrypt.hashpw(password, BCrypt.gensalt()));
    }

    public synchronized String generateTokenForUser(UserDto user) {
        byte[] bytes = new byte[128];
        secureTokenGenerator.nextBytes(bytes);
        String token = user.getId() + "_" + BaseEncoding.base64().encode(bytes);
        if (userAuthDao.setToken(user.getId(), token)) {
            return token;
        } else {
            return null;
        }
    }

    @Transactional
    public synchronized UserAuthDto createUserWithProfile(String name, SocialProfile socialProfile) {
        UserAuthDto result = userAuthDao.createUserWithProfile(name, socialProfile);
        journalService.userCreated(result.getUser());
        return result;
    }

    public synchronized UserAuthDto createUserAuthFromProfile(UserDto user, SocialProfile profile) {
        UserAuthDto userAuth = userAuthDao.createAuthFromProfile(profile, user);
        triggerAuthEvent(UserAuthEventType.CREATED, user, profile.getService());
        return userAuth;
    }

    public synchronized UserAuthDto getAuthDataForUser(UserDto user, String service) {
        return userAuthDao.getAuthDataForUser(user.getId(), service);
    }

    public synchronized UserAuthDto getAuthDataForUser(long userId, String service) {
        return userAuthDao.getAuthDataForUser(userId, service);
    }

    public boolean hasRole(Request request, GlobalRole role) {
        UserDto user = checkFullAuthentication(request);
        return user != null && user.hasRole(role);
    }

    public synchronized void deleteAuth(UserDto user, String serviceName) {
        userAuthDao.deleteAuth(user, serviceName);
        triggerAuthEvent(UserAuthEventType.DELETED, user, serviceName);
    }

    public Set<String> getBannedIps() {
        return this.bannedIps;
    }

    public void registerAuthEventListener(UserAuthEventListener listener) {
        userAuthEventListeners.add(listener);
    }

    private boolean validatePassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }

    private void triggerAuthEvent(UserAuthEventType type, UserDto user, String service) {
        userAuthEventListeners.forEach(listener -> listener.onEvent(type, user, service));
    }
}
