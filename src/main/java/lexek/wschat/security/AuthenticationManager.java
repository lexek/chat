package lexek.wschat.security;

import com.google.common.io.BaseEncoding;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.httpserver.Request;
import lexek.wschat.EmailConfiguration;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.SessionDto;
import lexek.wschat.db.UserAuthDto;
import lexek.wschat.db.UserDto;
import lexek.wschat.db.jooq.tables.pojos.PendingConfirmation;
import lexek.wschat.security.social.SocialAuthProfile;
import lexek.wschat.util.Colors;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.sql.DataSource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static lexek.wschat.db.jooq.tables.PendingConfirmation.PENDING_CONFIRMATION;
import static lexek.wschat.db.jooq.tables.Session.SESSION;
import static lexek.wschat.db.jooq.tables.User.USER;
import static lexek.wschat.db.jooq.tables.Userauth.USERAUTH;

public class AuthenticationManager {
    private final Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);
    private final Map<String, AtomicInteger> failedLogin = new ConcurrentHashMapV8<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final String host;
    private final DataSource dataSource;
    private final EmailConfiguration emailConfig;

    public AuthenticationManager(String host, DataSource dataSource, EmailConfiguration emailConfig) {
        this.host = host;
        this.dataSource = dataSource;
        this.emailConfig = emailConfig;
    }

    public UserAuthDto fastAuth(String name, String password, String ip) {
        AtomicInteger tries = failedLogin.get(ip);
        UserAuthDto auth = null;
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                    .select()
                    .from(USERAUTH)
                    .join(USER).on(USERAUTH.USER_ID.equal(USER.ID))
                    .where(USERAUTH.SERVICE.equal("password").and(USER.NAME.equal(name)))
                    .fetchOne();
            if (record != null) {
                auth = UserAuthDto.fromRecord(record);
            }
        } catch (DataAccessException | SQLException e) {
            auth = null;
            logger.warn("", e);
        }

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
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String sid = userAuth.getId() + "_" + BaseEncoding.base16().encode(bytes);

        SessionDto session = null;
        try (Connection connection = dataSource.getConnection()) {
            session = DSL.using(connection).transactionResult(txConf -> {
                Record record = DSL.using(txConf)
                        .insertInto(SESSION, SESSION.IP, SESSION.SID, SESSION.USERAUTH_ID, SESSION.EXPIRES)
                        .values(ip, sid, userAuth.getId(), timestamp + TimeUnit.DAYS.toMillis(30))
                        .returning().fetchOne();
                return SessionDto.fromRecord(record, userAuth);
            });
            session.setUserAuth(userAuth);
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
        return session;
    }

    public UserAuthDto getOrCreateUserAuth(SocialAuthProfile profile) {
        UserAuthDto auth = null;
        try (Connection connection = dataSource.getConnection()) {
            auth = DSL.using(connection).transactionResult(conf -> {
                UserAuthDto result;
                Record record = DSL.using(conf).select()
                        .from(USERAUTH)
                        .leftOuterJoin(USER).on(USERAUTH.USER_ID.equal(USER.ID))
                        .where(USERAUTH.SERVICE.equal(profile.getService())
                                .and(USERAUTH.AUTHID.equal(String.valueOf(profile.getId()))))
                        .fetchOne();
                result = UserAuthDto.fromRecord(record);
                if (result == null) {
                    record = DSL.using(conf)
                            .insertInto(USERAUTH, USERAUTH.AUTHNAME, USERAUTH.AUTHID, USERAUTH.AUTHKEY, USERAUTH.SERVICE)
                            .values(profile.getName(), String.valueOf(profile.getId()), profile.getToken(), profile.getService())
                            .returning().fetchOne();
                    result = UserAuthDto.fromRecord(record, null);
                } else {
                    DSL.using(conf)
                            .update(USERAUTH)
                            .set(USERAUTH.AUTHNAME, profile.getName())
                            .set(USERAUTH.AUTHKEY, profile.getToken())
                            .where(USERAUTH.SERVICE.equal(profile.getService()).and(USERAUTH.AUTHID.equal(String.valueOf(profile.getId()))))
                            .execute();
                }
                return result;
            });
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }

        return auth;
    }

    public void addAuth(UserDto user, SocialAuthProfile profile) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection).transaction(conf ->
                    DSL.using(conf)
                            .insertInto(USERAUTH, USERAUTH.AUTHNAME, USERAUTH.AUTHID, USERAUTH.AUTHKEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                            .values(profile.getName(), String.valueOf(profile.getId()), profile.getToken(), profile.getService(), user.getId())
                            .onDuplicateKeyUpdate()
                            .set(USERAUTH.USER_ID, USERAUTH.USER_ID.nvl(user.getId()))
                            .execute());
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public boolean tieUserWithExistingAuth(UserDto userDto, UserAuthDto userAuthDto) {
        boolean success = false;
        try (Connection connection = dataSource.getConnection()) {
            success = DSL.using(connection).transactionResult(conf ->
                    DSL.using(conf)
                            .update(USERAUTH)
                            .set(USERAUTH.USER_ID, userDto.getId())
                            .where(USERAUTH.ID.equal(userAuthDto.getId()).and(USERAUTH.USER_ID.isNull()))
                            .execute() == 1);
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
        return success;
    }

    public UserAuthDto checkAuthentication(String sid, String ip) {
        UserAuthDto auth = null;
        SessionDto session = null;

        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                    .select()
                    .from(SESSION)
                    .join(USERAUTH).on(SESSION.USERAUTH_ID.eq(USERAUTH.ID))
                    .leftOuterJoin(USER).on(USERAUTH.USER_ID.equal(USER.ID))
                    .where(SESSION.SID.equal(sid)
                            .and(SESSION.IP.equal(ip))
                            .and(SESSION.EXPIRES.greaterOrEqual(System.currentTimeMillis())))
                    .fetchOne();
            session = SessionDto.fromRecord(record);
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }

        if (session != null) {
            auth = session.getUserAuth();
        }
        return auth;
    }

    public UserAuthDto checkAuthentication(Request request) {
        String sid = request.cookieValue("sid");
        UserAuthDto user = null;
        if (sid != null) {
            user = checkAuthentication(sid, request.ip());
        }
        return user;
    }

    private String generateConfirmationCode() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return BaseEncoding.base32Hex().encode(bytes);
    }

    private void sendConfirmationEmail(String email, String code) {
        Session session = Session.getInstance(System.getProperties(), null);
        MimeMessage message = new MimeMessage(session);

        try {
            InternetAddress from = new InternetAddress(emailConfig.getEmail());
            message.setSubject("Confirm your email.");
            message.setFrom(from);
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(email));

            Multipart multipart = new MimeMultipart("alternative");
            BodyPart messageBodyPart = new MimeBodyPart();
            String link = "https://" + host + ":1337/confirm_email?code=" + URLEncoder.encode(code, "utf-8");
            messageBodyPart.setText(link);
            multipart.addBodyPart(messageBodyPart);

            message.setContent(multipart);
            Transport transport = session.getTransport("smtps");
            transport.connect(emailConfig.getSmtpHost(), emailConfig.getSmtpPort(), emailConfig.getEmail(), emailConfig.getPassword());
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("exception", e);
        }
    }


    public boolean registerWithPassword(final String name, final String password, final String email) {
        boolean success = false;
        final String color = Colors.generateColor(name);
        try (Connection connection = dataSource.getConnection()) {
            final DSLContext dslContext = DSL.using(connection);
            String confirmationCode = dslContext.transactionResult(conf -> {
                String code = null;
                Long id = DSL.using(conf)
                        .insertInto(USER, USER.NAME, USER.BANNED, USER.COLOR, USER.RENAMEAVAILABLE, USER.ROLE, USER.EMAIL)
                        .values(name, false, color, false, GlobalRole.USER_UNCONFIRMED.toString(), email)
                        .returning(USER.ID)
                        .fetchOne().getValue(USER.ID);
                if (id != null) {
                    DSL.using(conf)
                            .insertInto(USERAUTH, USERAUTH.AUTHNAME, USERAUTH.AUTHID, USERAUTH.AUTHKEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                            .values(null, null, password, "password", id)
                            .execute();
                    code = generateConfirmationCode();
                    DSL.using(conf)
                            .insertInto(PENDING_CONFIRMATION, PENDING_CONFIRMATION.CODE, PENDING_CONFIRMATION.USER)
                            .values(code, id)
                            .execute();
                }
                return code;
            });
            success = confirmationCode != null;
            if (success) {
                sendConfirmationEmail(email, confirmationCode);
            }
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
        return success;
    }

    public boolean confirmEmail(final String code) {
        boolean success = false;
        try (Connection connection = dataSource.getConnection()) {
            success = DSL.using(connection).transactionResult(conf -> {
                boolean success1 = false;
                Record record = DSL.using(conf)
                        .select()
                        .from(PENDING_CONFIRMATION)
                        .where(PENDING_CONFIRMATION.CODE.equal(code))
                        .fetchOne();
                if (record != null) {
                    PendingConfirmation pendingConfirmation = record.into(PendingConfirmation.class);
                    DSL.using(conf)
                            .update(USER)
                            .set(USER.ROLE, GlobalRole.USER.toString())
                            .where(USER.ID.equal(pendingConfirmation.getUser()))
                            .execute();
                    DSL.using(conf)
                            .delete(PENDING_CONFIRMATION)
                            .where(PENDING_CONFIRMATION.ID.equal(pendingConfirmation.getId()));
                    success1 = true;
                }
                return success1;
            });
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
        return success;
    }

    public void setPassword(long userId, String password) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                    .insertInto(USERAUTH, USERAUTH.AUTHNAME, USERAUTH.AUTHID, USERAUTH.AUTHKEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                    .values(null, null, password, "password", userId)
                    .onDuplicateKeyUpdate()
                    .set(USERAUTH.AUTHKEY, password)
                    .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public boolean addUserAndTieToAuth(final String name, final UserAuthDto auth) {
        boolean success = false;
        final String color = Colors.generateColor(name);
        try (Connection connection = dataSource.getConnection()) {
            success = DSL.using(connection).transactionResult(conf -> {
                Long id = DSL.using(conf)
                        .insertInto(USER, USER.NAME, USER.BANNED, USER.COLOR, USER.RENAMEAVAILABLE, USER.ROLE)
                        .values(name, false, color, false, GlobalRole.USER.toString())
                        .returning()
                        .fetchOne()
                        .getValue(USER.ID);
                if (id != null) {
                    DSL.using(conf)
                            .update(USERAUTH)
                            .set(USERAUTH.USER_ID, id)
                            .where(USERAUTH.ID.equal(auth.getId()))
                            .execute();
                    return true;
                }
                return false;
            });
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
        return success;
    }

    public UserAuthDto getAuthDataForUser(long id, String service) {
        UserAuthDto auth = null;
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                    .select(USERAUTH.AUTHKEY, USERAUTH.AUTHNAME)
                    .from(USERAUTH)
                    .join(USER).on(USER.ID.equal(USERAUTH.USER_ID))
                    .where(USER.ID.equal(id).and(USERAUTH.SERVICE.equal(service)))
                    .fetchOne();
            if (record != null) {
                auth = new UserAuthDto(0, null, null, null, record.getValue(USERAUTH.AUTHKEY), record.getValue(USERAUTH.AUTHNAME));
            }
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return auth;
    }

    public boolean hasRole(Request request, GlobalRole role) {
        UserAuthDto auth = checkAuthentication(request);
        return auth != null && auth.getUser() != null && auth.getUser().getRole().compareTo(role) >= 0;
    }

    public void invalidate(String sid) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                    .delete(SESSION)
                    .where(SESSION.SID.equal(sid))
                    .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
    }
}
