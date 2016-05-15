package lexek.wschat.db.dao;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.PendingConfirmation;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.util.Colors;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static lexek.wschat.db.jooq.tables.PendingConfirmation.PENDING_CONFIRMATION;
import static lexek.wschat.db.jooq.tables.Session.SESSION;
import static lexek.wschat.db.jooq.tables.User.USER;
import static lexek.wschat.db.jooq.tables.Userauth.USERAUTH;

public class UserAuthDao {
    private final Logger logger = LoggerFactory.getLogger(UserAuthDao.class);
    private final DataSource dataSource;

    public UserAuthDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public UserAuthDto getPasswordAuth(String name) {
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
        return auth;
    }

    public UserAuthDto getTokenAuth(String token) {
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

    public UserAuthDto getOrCreateUserAuth(SocialProfile profile) {
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
                        .values(profile.getName(), String.valueOf(profile.getId()), profile.getToken().getToken(), profile.getService())
                        .returning().fetchOne();
                    result = UserAuthDto.fromRecord(record, null);
                } else {
                    DSL.using(conf)
                        .update(USERAUTH)
                        .set(USERAUTH.AUTH_NAME, profile.getName())
                        .set(USERAUTH.AUTH_KEY, profile.getToken().getToken())
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

    public void tieUserWithExistingAuth(UserDto userDto, UserAuthDto userAuthDto) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection).transaction(conf -> DSL.using(conf)
                .update(USERAUTH)
                .set(USERAUTH.USER_ID, userDto.getId())
                .where(USERAUTH.ID.equal(userAuthDto.getId()).and(USERAUTH.USER_ID.isNull()))
                .execute()
            );
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public Long registerWithPassword(String name, String password, String email, String color, String verificationCode) {
        Long result = null;
        try (Connection connection = dataSource.getConnection()) {
            final DSLContext dslContext = DSL.using(connection);
            result = dslContext.transactionResult(conf -> {
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
                    DSL.using(conf)
                        .insertInto(PENDING_CONFIRMATION, PENDING_CONFIRMATION.CODE, PENDING_CONFIRMATION.USER_ID)
                        .values(verificationCode, id)
                        .execute();
                }
                return id;
            });
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
        return result;
    }

    public boolean setEmail(long userId, String email, String verificationCode) {
        boolean success = false;
        try (Connection connection = dataSource.getConnection()) {
            final DSLContext dslContext = DSL.using(connection);
            success = dslContext.transactionResult(conf -> {
                DSL.using(conf)
                    .update(USER)
                    .set(USER.EMAIL, email)
                    .set(USER.EMAIL_VERIFIED, false)
                    .where(USER.ID.equal(userId))
                    .execute();
                DSL.using(conf)
                    .insertInto(PENDING_CONFIRMATION, PENDING_CONFIRMATION.CODE, PENDING_CONFIRMATION.USER_ID)
                    .values(verificationCode, userId)
                    .onDuplicateKeyUpdate()
                    .set(PENDING_CONFIRMATION.CODE, verificationCode)
                    .execute();
                return true;
            });
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
        return success;
    }

    public void setPassword(long userId, String passwordHash) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .insertInto(USERAUTH, USERAUTH.AUTH_NAME, USERAUTH.AUTH_ID, USERAUTH.AUTH_KEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                .values(null, null, passwordHash, "password", userId)
                .onDuplicateKeyUpdate()
                .set(USERAUTH.AUTH_KEY, passwordHash)
                .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public boolean setToken(long userId, String token) {
        boolean success = false;
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .insertInto(USERAUTH, USERAUTH.AUTH_NAME, USERAUTH.AUTH_ID, USERAUTH.AUTH_KEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                .values(null, null, token, "token", userId)
                .onDuplicateKeyUpdate()
                .set(USERAUTH.AUTH_KEY, token)
                .execute();
            success = true;
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
        return success;
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

    public SessionDto getSession(String sid, String ip) {
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
        return session;
    }

    public SessionDto newSession(String sid, String ip, UserAuthDto userAuth, long timestamp) {
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

    public boolean verifyEmail(final String code, long userId) {
        boolean success = false;
        try (Connection connection = dataSource.getConnection()) {
            success = DSL.using(connection).transactionResult(conf -> {
                Record record = DSL.using(conf)
                    .select()
                    .from(PENDING_CONFIRMATION.join(USER).on(PENDING_CONFIRMATION.USER_ID.equal(USER.ID)))
                    .where(PENDING_CONFIRMATION.CODE.equal(code).and(USER.ID.equal(userId)))
                    .fetchOne();
                if (record != null) {
                    PendingConfirmation pendingConfirmation = record.into(PENDING_CONFIRMATION).into(PendingConfirmation.class);
                    UserDto user = UserDto.fromRecord(record.into(USER));
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
                    return true;
                }
                return false;
            });
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
        return success;
    }

    public void invalidateSession(String sid) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .delete(SESSION)
                .where(SESSION.SID.equal(sid))
                .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public String getPendingVerificationCode(long id) {
        String code = null;
        try (Connection connection = dataSource.getConnection()) {
            code = DSL.using(connection)
                .select(PENDING_CONFIRMATION.CODE)
                .from(PENDING_CONFIRMATION)
                .where(PENDING_CONFIRMATION.USER_ID.equal(id))
                .fetchOne()
                .value1();
        } catch (DataAccessException | SQLException e) {
            logger.error(e.getMessage());
        }
        return code;
    }
}
