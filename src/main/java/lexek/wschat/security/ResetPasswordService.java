package lexek.wschat.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lexek.wschat.chat.e.BadRequestException;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.model.Email;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.tx.Transactional;
import lexek.wschat.services.EmailService;
import lexek.wschat.services.JournalService;
import lexek.wschat.services.UserService;
import lexek.wschat.util.Names;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

@Service
public class ResetPasswordService {
    private final Logger logger = LoggerFactory.getLogger(ResetPasswordService.class);
    private final String host;
    private final UserService userService;
    private final Cache<String, Long> tokens;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final SessionService sessionService;
    private final SecureTokenGenerator secureTokenGenerator;
    private final JournalService journalService;

    @Inject
    public ResetPasswordService(
        @Named("core.hostname") String host,
        UserService userService,
        EmailService emailService,
        AuthenticationManager authenticationManager,
        SessionService sessionService,
        SecureTokenGenerator secureTokenGenerator,
        JournalService journalService
    ) {
        this.host = host;
        this.userService = userService;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.sessionService = sessionService;
        this.secureTokenGenerator = secureTokenGenerator;
        this.journalService = journalService;
        this.tokens = CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES).build();
    }

    public void requestPasswordReset(String name) {
        UserDto user = userService.findByNameOrEmail(name.toLowerCase());
        if (user == null) {
            logger.debug("user not found for name {}", name);
            return;
        }
        logger.info("password reset requested for {} ({})", user.getName(), user.getId());
        String token = secureTokenGenerator.generateRandomToken(32);
        tokens.put(token, user.getId());
        try {
            emailService.sendEmail(new Email(
                user.getEmail(),
                "Password reset",
                "https://" + host + ":1337/rest/auth/forgotPassword" +
                    "?token=" + URLEncoder.encode(token, "utf-8") +
                    "&uid=" + user.getId()
            ));
        } catch (UnsupportedEncodingException e) {
            logger.warn("unable to send email", e);
        }
    }

    @Transactional
    public synchronized void resetPassword(String token, long userId, String newPassword) {
        Long storedUserId = tokens.getIfPresent(token);
        if (storedUserId == null || userId != storedUserId) {
            throw new BadRequestException("Invalid token");
        }
        if (!Names.PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BadRequestException("Bad password format.");
        }
        User user = userService.fetchById(userId);
        authenticationManager.setPasswordNoCheck(user, newPassword);
        sessionService.invalidateUserSessions(user);
        logger.info("reset password for user {} ({})", user.getName(), user.getId());
        journalService.userPasswordReset(user);
        tokens.invalidate(token);
    }
}
