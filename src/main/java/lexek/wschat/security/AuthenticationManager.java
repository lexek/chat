package lexek.wschat.security;

import com.google.common.io.BaseEncoding;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.httpserver.Request;
import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.dao.UserAuthDao;
import lexek.wschat.db.model.Email;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.social.SocialAuthProfile;
import lexek.wschat.services.EmailService;
import lexek.wschat.util.Colors;
import org.apache.http.HttpHeaders;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AuthenticationManager {
    private final Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);
    private final Map<String, AtomicInteger> failedLogin = new ConcurrentHashMapV8<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final String host;
    private final EmailService emailService;
    private final ConnectionManager connectionManager;
    private final UserAuthDao userAuthDao;

    public AuthenticationManager(String host, EmailService emailService, ConnectionManager connectionManager,
                                 UserAuthDao userAuthDao) {
        this.host = host;
        this.emailService = emailService;
        this.connectionManager = connectionManager;
        this.userAuthDao = userAuthDao;
    }

    public UserAuthDto fastAuth(String name, String password, String ip) {
        AtomicInteger tries = failedLogin.get(ip);
        UserAuthDto auth = userAuthDao.getPasswordAuth(name);
        if (auth != null && auth.getAuthenticationKey() != null && BCrypt.checkpw(password, auth.getAuthenticationKey())) {
            return auth;
        } else {
            if (tries == null) {
                tries = new AtomicInteger(1);
                failedLogin.put(ip, tries);
            } else {
                tries.incrementAndGet();
            }
            return null;
        }
    }

    public UserAuthDto fastAuthToken(String token) {
        return userAuthDao.getTokenAuth(token);
    }

    public int failedLoginTries(String ip) {
        AtomicInteger i = failedLogin.get(ip);
        return i != null ? i.get() : 0;
    }

    public SessionDto authenticate(String username, String password, String ip, long timestamp) {
        UserAuthDto user = fastAuth(username, password, ip);
        if (user != null) {
            return createSession(user, ip, timestamp);
        }
        return null;
    }

    public SessionDto createSession(UserAuthDto userAuth, String ip, long timestamp) {
        if (userAuth == null || ip == null) {
            throw new NullPointerException();
        }
        String sid = userAuth.getId() + "_" + generateSessionId();
        return userAuthDao.newSession(
            sid,
            ip,
            userAuth,
            timestamp
        );
    }

    public UserAuthDto getOrCreateUserAuth(SocialAuthProfile profile) {
        return userAuthDao.getOrCreateUserAuth(profile);
    }

    public boolean tieUserWithExistingAuth(UserDto userDto, UserAuthDto userAuthDto) {
        return userAuthDao.tieUserWithExistingAuth(userDto, userAuthDto);
    }

    public UserAuthDto checkFullAuthentication(String sid, String ip) {
        UserAuthDto auth = null;
        SessionDto session = userAuthDao.getSession(sid, ip);
        if (session != null) {
            auth = session.getUserAuth();
        }
        return auth;
    }

    public UserAuthDto checkFullAuthentication(Request request) {
        UserAuthDto user = null;
        if (request.hasHeader(HttpHeaders.AUTHORIZATION)) {
            String[] tmp = request.header(HttpHeaders.AUTHORIZATION).split(" ", 2);
            if (tmp.length == 2) {
                String type = tmp[0];
                String token = tmp[1];
                if (type.equals("Token")) {
                    user = fastAuthToken(token);
                }
            }
        }
        if (user == null) {
            String sid = request.cookieValue("sid");
            if (sid != null) {
                user = checkFullAuthentication(sid, request.ip());
            }
        }
        return user;
    }

    public UserDto checkAuthentication(Request request) {
        UserAuthDto auth = checkFullAuthentication(request);
        if (auth != null && auth.getUser() != null) {
            return auth.getUser();
        } else {
            return null;
        }
    }

    private String generateVerificationCode() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return BaseEncoding.base32Hex().encode(bytes);
    }

    private String generateSessionId() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return BaseEncoding.base16().encode(bytes);
    }

    public boolean registerWithPassword(final String name, final String password, final String email) {
        final String color = Colors.generateColor(name);
        String verificationCode = generateVerificationCode();
        boolean success = userAuthDao.registerWithPassword(name, password, email, color, verificationCode);
        if (success) {
            sendVerificationEmail(email, verificationCode);
        }
        return success;
    }

    public void setEmail(UserDto user, String email) {
        if (!user.isEmailVerified()) {
            String verificationCode = generateVerificationCode();
            if (userAuthDao.setEmail(user.getId(), email, verificationCode)) {
                user.setEmail(email);
                user.setEmailVerified(false);
                connectionManager.forEach(c -> user.getId().equals(c.getUser().getId()), lexek.wschat.chat.Connection::close);
                sendVerificationEmail(email, verificationCode);
            }
        }
    }

    private void sendVerificationEmail(String email, String verificationCode) {
        try {
            emailService.sendEmail(new Email(
                email,
                "Verify your email.",
                "https://" + host + ":1337/confirm_email?code=" + URLEncoder.encode(verificationCode, "utf-8")
            ));
        } catch (UnsupportedEncodingException e) {
            logger.warn("", e);
        }
    }

    public boolean verifyEmail(final String code) {
        return userAuthDao.verifyEmail(code);
    }

    public void setPassword(UserDto user, String password) {
        userAuthDao.setPassword(user.getId(), BCrypt.hashpw(password, BCrypt.gensalt()));
    }

    public String createTokenForUser(UserDto user) {
        byte[] bytes = new byte[128];
        secureRandom.nextBytes(bytes);
        String token = user.getId() + "_" + BaseEncoding.base64().encode(bytes);
        if (userAuthDao.setToken(user.getId(), token)) {
            return token;
        } else {
            return null;
        }
    }

    public boolean addUserAndTieToAuth(String name, UserAuthDto auth) {
        return userAuthDao.addUserAndTieToAuth(name, auth);
    }

    public UserAuthDto getAuthDataForUser(UserDto user, String service) {
        return userAuthDao.getAuthDataForUser(user.getId(), service);
    }

    public boolean hasRole(Request request, GlobalRole role) {
        UserAuthDto auth = checkFullAuthentication(request);
        return auth != null && auth.getUser() != null && auth.getUser().getRole().compareTo(role) >= 0;
    }

    public void invalidateSession(String sid) {
        userAuthDao.invalidateSession(sid);
    }
}
