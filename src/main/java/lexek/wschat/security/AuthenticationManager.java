package lexek.wschat.security;

import com.google.common.io.BaseEncoding;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.httpserver.Request;
import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.dao.UserAuthDao;
import lexek.wschat.db.model.Email;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.tx.Transactional;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.services.EmailService;
import lexek.wschat.util.Colors;
import org.apache.http.HttpHeaders;
import org.jvnet.hk2.annotations.Service;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AuthenticationManager {
    private final Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);
    private final Map<String, AtomicInteger> failedLogin = new ConcurrentHashMapV8<>();
    private final Map<Long, Instant> latestEmailChanges = new ConcurrentHashMapV8<>();
    private final Set<UserAuthEventListener> userAuthEventListeners = new HashSet<>();
    private final SecureTokenGenerator secureTokenGenerator;
    private final String host;
    private final EmailService emailService;
    private final ConnectionManager connectionManager;
    private final UserAuthDao userAuthDao;
    private final Set<String> bannedIps = new CopyOnWriteArraySet<>();

    @Inject
    public AuthenticationManager(
        @Named("core.hostname") String host,
        SecureTokenGenerator secureTokenGenerator,
        EmailService emailService,
        ConnectionManager connectionManager,
        UserAuthDao userAuthDao
    ) {
        this.host = host;
        this.secureTokenGenerator = secureTokenGenerator;
        this.emailService = emailService;
        this.connectionManager = connectionManager;
        this.userAuthDao = userAuthDao;
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

    private boolean validatePassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }

    public UserDto fastAuthToken(String token) {
        return userAuthDao.getUserForToken(token);
    }

    public int failedLoginTries(String ip) {
        AtomicInteger i = failedLogin.get(ip);
        return i != null ? i.get() : 0;
    }

    @Transactional
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
        String sid = userAuth.getId() + "_" + secureTokenGenerator.generateSessionId();
        return userAuthDao.newSession(
            sid,
            ip,
            userAuth,
            System.currentTimeMillis()
        );
    }

    @Transactional
    public UserAuthDto getOrCreateUserAuth(SocialProfile profile, UserDto user) {
        return userAuthDao.getOrCreateUserAuth(profile, user);
    }

    @Transactional
    public UserDto checkFullAuthentication(String sid, String ip) {
        UserDto user = null;
        SessionDto session = userAuthDao.getSession(sid, ip);
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

    @Deprecated
    public UserDto checkAuthentication(Request request) {
        return checkFullAuthentication(request);
    }

    public synchronized boolean registerWithPassword(final String name, final String password, final String email) {
        final String color = Colors.generateColor(name);
        String verificationCode = secureTokenGenerator.generateVerificationCode();
        Long userId = userAuthDao.registerWithPassword(name, password, email, color, verificationCode);
        if (userId != null) {
            sendVerificationEmail(email, verificationCode, userId);
            latestEmailChanges.put(userId, Instant.now());
        }
        return userId != null;
    }

    public synchronized void setEmail(UserDto user, String email) {
        if (email == null) {
            throw new InvalidInputException("email", "ERROR_EMAIL_NULL");
        }

        if (email.equals(user.getEmail())) {
            throw new InvalidInputException("email", "ERROR_EMAIL_SAME");
        }

        Instant lastChange = latestEmailChanges.get(user.getId());
        if (lastChange != null) {
            Instant now = Instant.now();
            if (now.minus(30, ChronoUnit.MINUTES).isBefore(lastChange)) {
                throw new InvalidInputException("email", "ERROR_EMAIL_TOO_OFTEN");
            }
        }

        latestEmailChanges.put(user.getId(), Instant.now());
        doChangeEmail(user, email);
    }

    @Transactional
    private void doChangeEmail(UserDto user, String email) {
        String verificationCode = secureTokenGenerator.generateVerificationCode();
        if (userAuthDao.setEmail(user.getId(), email, verificationCode)) {
            user.setEmail(email);
            user.setEmailVerified(false);
            connectionManager.forEach(c -> user.getId().equals(c.getUser().getId()), lexek.wschat.chat.Connection::close);
            sendVerificationEmail(email, verificationCode, user.getId());
        }
    }

    public void resendVerificationEmail(UserDto user) {
        String code = userAuthDao.getPendingVerificationCode(user.getId());
        if (code != null) {
            sendVerificationEmail(user.getEmail(), code, user.getId());
        }
    }

    private void sendVerificationEmail(String email, String verificationCode, long userId) {
        try {
            emailService.sendEmail(new Email(
                email,
                "Verify your email.",
                "https://" + host + ":1337/rest/email/verify?code=" + URLEncoder.encode(verificationCode, "utf-8")
                    + "&uid=" + userId
            ));
        } catch (UnsupportedEncodingException e) {
            logger.warn("", e);
        }
    }

    @Transactional
    public synchronized boolean verifyEmail(String code, long userId) {
        boolean success = userAuthDao.verifyEmail(code, userId);
        if (success) {
            connectionManager.forEach(connection -> {
                Long id = connection.getUser().getId();
                if (id != null && id.equals(userId)) {
                    connection.close();
                }
            });
        }
        return success;
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

    public synchronized String createTokenForUser(UserDto user) {
        byte[] bytes = new byte[128];
        secureTokenGenerator.nextBytes(bytes);
        String token = user.getId() + "_" + BaseEncoding.base64().encode(bytes);
        if (userAuthDao.setToken(user.getId(), token)) {
            return token;
        } else {
            return null;
        }
    }

    public synchronized UserAuthDto createUserWithProfile(String name, SocialProfile socialProfile) {
        return userAuthDao.createUserWithProfile(name, socialProfile);
    }

    public synchronized UserAuthDto createUserAuthFromProfile(UserDto user, SocialProfile profile) {
        UserAuthDto userAuth = userAuthDao.createAuthFromProfile(user, profile);
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

    public void invalidateSession(String sid) {
        userAuthDao.invalidateSession(sid);
    }

    public synchronized void deleteAuth(UserDto user, String serviceName) {
        userAuthDao.deleteAuth(user, serviceName);
        triggerAuthEvent(UserAuthEventType.DELETED, user, serviceName);
    }

    public Set<String> getBannedIps() {
        return this.bannedIps;
    }

    private void triggerAuthEvent(UserAuthEventType type, UserDto user, String service) {
        userAuthEventListeners.forEach(listener -> listener.onEvent(type, user, service));
    }

    public void registerAuthEventListener(UserAuthEventListener listener) {
        userAuthEventListeners.add(listener);
    }
}
