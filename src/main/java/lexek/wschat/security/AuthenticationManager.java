package lexek.wschat.security;

import com.google.common.io.BaseEncoding;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.httpserver.Request;
import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.PendingConfirmation;
import lexek.wschat.db.model.Email;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.social.SocialAuthProfile;
import lexek.wschat.services.EmailService;
import lexek.wschat.util.Colors;
import org.apache.http.HttpHeaders;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final EmailService emailService;
    private final ConnectionManager connectionManager;

    public AuthenticationManager(String host, DataSource dataSource, EmailService emailService, ConnectionManager connectionManager) {
        this.host = host;
        this.dataSource = dataSource;
        this.emailService = emailService;
        this.connectionManager = connectionManager;
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

    public UserAuthDto fastAuthToken(String token) {
        UserAuthDto auth = null;
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                .select()
                .from(USERAUTH)
                .join(USER).on(USERAUTH.USER_ID.equal(USER.ID))
                .where(USERAUTH.SERVICE.equal("token").and(USERAUTH.AUTH_KEY.equal(token)))
                .fetchOne();
            if (record != null) {
                auth = UserAuthDto.fromRecord(record);
            }
        } catch (DataAccessException | SQLException e) {
            auth = null;
            logger.warn("", e);
        }

        return auth;
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
                        .and(USERAUTH.AUTH_ID.equal(String.valueOf(profile.getId()))))
                    .fetchOne();
                result = UserAuthDto.fromRecord(record);
                if (result == null) {
                    record = DSL.using(conf)
                        .insertInto(USERAUTH, USERAUTH.AUTH_NAME, USERAUTH.AUTH_ID, USERAUTH.AUTH_KEY, USERAUTH.SERVICE)
                        .values(profile.getName(), String.valueOf(profile.getId()), profile.getToken(), profile.getService())
                        .returning().fetchOne();
                    result = UserAuthDto.fromRecord(record, null);
                } else {
                    DSL.using(conf)
                        .update(USERAUTH)
                        .set(USERAUTH.AUTH_NAME, profile.getName())
                        .set(USERAUTH.AUTH_KEY, profile.getToken())
                        .where(USERAUTH.SERVICE.equal(profile.getService()).and(USERAUTH.AUTH_ID.equal(String.valueOf(profile.getId()))))
                        .execute();
                }
                return result;
            });
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }

        return auth;
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

    public UserAuthDto checkFullAuthentication(String sid, String ip) {
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

    private String generateConfirmationCode() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return BaseEncoding.base32Hex().encode(bytes);
    }

    public boolean registerWithPassword(final String name, final String password, final String email) {
        boolean success = false;
        final String color = Colors.generateColor(name);
        try (Connection connection = dataSource.getConnection()) {
            final DSLContext dslContext = DSL.using(connection);
            String confirmationCode = dslContext.transactionResult(conf -> {
                String code = null;
                Long id = DSL.using(conf)
                    .insertInto(USER, USER.NAME, USER.BANNED, USER.COLOR, USER.RENAME_AVAILABLE, USER.ROLE, USER.EMAIL, USER.EMAIL_VERIFIED)
                    .values(name, false, color, false, GlobalRole.USER_UNCONFIRMED.toString(), email, false)
                    .returning(USER.ID)
                    .fetchOne().getValue(USER.ID);
                if (id != null) {
                    DSL.using(conf)
                        .insertInto(USERAUTH, USERAUTH.AUTH_NAME, USERAUTH.AUTH_ID, USERAUTH.AUTH_KEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                        .values(null, null, password, "password", id)
                        .execute();
                    code = generateConfirmationCode();
                    DSL.using(conf)
                        .insertInto(PENDING_CONFIRMATION, PENDING_CONFIRMATION.CODE, PENDING_CONFIRMATION.USER_ID)
                        .values(code, id)
                        .execute();
                }
                return code;
            });
            success = confirmationCode != null;
            if (success) {
                emailService.sendEmail(new Email(
                    email,
                    "Verify your email.",
                    "https://" + host + ":1337/confirm_email?code=" + URLEncoder.encode(confirmationCode, "utf-8")
                ));
            }
        } catch (DataAccessException | SQLException | UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }
        return success;
    }

    public void setEmail(UserDto user, String email) {
        if (!user.isEmailVerified()) {
            try (Connection connection = dataSource.getConnection()) {
                final DSLContext dslContext = DSL.using(connection);
                String confirmationCode = dslContext.transactionResult(conf -> {
                    String code = generateConfirmationCode();
                    DSL.using(conf)
                        .update(USER)
                        .set(USER.EMAIL, email)
                        .set(USER.EMAIL_VERIFIED, false)
                        .where(USER.ID.equal(user.getId()))
                        .execute();
                    DSL.using(conf)
                        .insertInto(PENDING_CONFIRMATION, PENDING_CONFIRMATION.CODE, PENDING_CONFIRMATION.USER_ID)
                        .values(code, user.getId())
                        .execute();
                    return code;
                });
                if (confirmationCode != null) {
                    user.setEmail(email);
                    user.setEmailVerified(false);
                    connectionManager.forEach(c -> user.getId().equals(c.getUser().getId()), lexek.wschat.chat.Connection::close);
                    emailService.sendEmail(new Email(
                        user.getEmail(),
                        "Verify your email.",
                        "https://" + host + ":1337/confirm_email?code=" + URLEncoder.encode(confirmationCode, "utf-8")
                    ));
                }
            } catch (DataAccessException | SQLException | UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
        }
    }

    public boolean confirmEmail(UserDto user, final String code) {
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
                    if (user.hasRole(GlobalRole.USER)) {
                        DSL.using(conf)
                            .update(USER)
                            .set(USER.EMAIL_VERIFIED, true)
                            .where(USER.ID.equal(pendingConfirmation.getUserId()))
                            .execute();
                    } else {
                        DSL.using(conf)
                            .update(USER)
                            .set(USER.ROLE, GlobalRole.USER.toString())
                            .set(USER.EMAIL_VERIFIED, true)
                            .where(USER.ID.equal(pendingConfirmation.getUserId()))
                            .execute();
                    }
                    DSL.using(conf)
                        .delete(PENDING_CONFIRMATION)
                        .where(PENDING_CONFIRMATION.ID.equal(pendingConfirmation.getId()))
                        .execute();
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
                .insertInto(USERAUTH, USERAUTH.AUTH_NAME, USERAUTH.AUTH_ID, USERAUTH.AUTH_KEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                .values(null, null, password, "password", userId)
                .onDuplicateKeyUpdate()
                .set(USERAUTH.AUTH_KEY, password)
                .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public String createTokenForUser(long userId) {
        byte[] bytes = new byte[128];
        secureRandom.nextBytes(bytes);
        String token = userId + "_" + BaseEncoding.base64().encode(bytes);
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .insertInto(USERAUTH, USERAUTH.AUTH_NAME, USERAUTH.AUTH_ID, USERAUTH.AUTH_KEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                .values(null, null, token, "token", userId)
                .onDuplicateKeyUpdate()
                .set(USERAUTH.AUTH_KEY, token)
                .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
            token = null;
        }
        return token;
    }

    public boolean addUserAndTieToAuth(final String name, final UserAuthDto auth) {
        boolean success = false;
        final String color = Colors.generateColor(name);
        try (Connection connection = dataSource.getConnection()) {
            success = DSL.using(connection).transactionResult(conf -> {
                Long id = DSL.using(conf)
                    .insertInto(USER, USER.NAME, USER.BANNED, USER.COLOR, USER.RENAME_AVAILABLE, USER.ROLE, USER.EMAIL, USER.EMAIL_VERIFIED)
                    .values(name, false, color, false, GlobalRole.USER.toString(), null, false)
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
                .select(USERAUTH.AUTH_KEY, USERAUTH.AUTH_NAME)
                .from(USERAUTH)
                .join(USER).on(USER.ID.equal(USERAUTH.USER_ID))
                .where(USER.ID.equal(id).and(USERAUTH.SERVICE.equal(service)))
                .fetchOne();
            if (record != null) {
                auth = new UserAuthDto(0, null, null, null, record.getValue(USERAUTH.AUTH_KEY), record.getValue(USERAUTH.AUTH_NAME));
            }
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return auth;
    }

    public boolean hasRole(Request request, GlobalRole role) {
        UserAuthDto auth = checkFullAuthentication(request);
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
