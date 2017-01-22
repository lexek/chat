package lexek.wschat.security;

import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.db.dao.UserAuthDao;
import lexek.wschat.db.model.Email;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.EmailService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class UserEmailManager {
    private final Logger logger = LoggerFactory.getLogger(UserEmailManager.class);
    private final String host;
    private final EmailService emailService;
    private final UserAuthDao userAuthDao;
    private final ConnectionManager connectionManager;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Map<Long, Instant> latestEmailChanges = new ConcurrentHashMapV8<>();

    @Inject
    public UserEmailManager(
        @Named("core.hostname") String host,
        EmailService emailService,
        UserAuthDao userAuthDao,
        ConnectionManager connectionManager,
        SecureTokenGenerator secureTokenGenerator
    ) {
        this.host = host;
        this.emailService = emailService;
        this.userAuthDao = userAuthDao;
        this.connectionManager = connectionManager;
        this.secureTokenGenerator = secureTokenGenerator;
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

        String verificationCode = secureTokenGenerator.generateVerificationCode();
        userAuthDao.setEmail(user.getId(), email, verificationCode);
        user.setEmail(email);
        user.setEmailVerified(false);
        connectionManager.forEach(c -> user.getId().equals(c.getUser().getId()), lexek.wschat.chat.Connection::close);
        sendVerificationEmail(email, verificationCode, user.getId());
        latestEmailChanges.put(user.getId(), Instant.now());
    }

    public void resendVerificationEmail(UserDto user) {
        String code = userAuthDao.getPendingVerificationCode(user.getId());
        if (code != null) {
            sendVerificationEmail(user.getEmail(), code, user.getId());
        }
    }

    public void sendVerificationEmail(String email, String verificationCode, long userId) {
        try {
            emailService.sendEmail(new Email(
                email,
                "Verify your email.",
                "https://" + host + ":1337/rest/email/verify" +
                    "?code=" + URLEncoder.encode(verificationCode, "utf-8") +
                    "&uid=" + userId
            ));
        } catch (UnsupportedEncodingException e) {
            logger.warn("", e);
        }
    }

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

    public void userCreated(UserDto user) {
        latestEmailChanges.put(user.getId(), Instant.now());
    }
}
