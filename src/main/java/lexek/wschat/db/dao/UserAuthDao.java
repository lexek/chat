package lexek.wschat.db.dao;

import lexek.wschat.chat.e.BadRequestException;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.PendingConfirmation;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.tx.Transactional;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.util.Colors;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static lexek.wschat.db.jooq.tables.PendingConfirmation.PENDING_CONFIRMATION;
import static lexek.wschat.db.jooq.tables.User.USER;
import static lexek.wschat.db.jooq.tables.Userauth.USERAUTH;

@Service
public class UserAuthDao {
    private final Logger logger = LoggerFactory.getLogger(UserAuthDao.class);
    private final DSLContext ctx;

    @Inject
    public UserAuthDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public UserAuthDto getPasswordAuth(String name) {
        UserAuthDto auth = null;
        try {
            Record record = ctx
                .select()
                .from(USERAUTH)
                .join(USER).on(USERAUTH.USER_ID.equal(USER.ID))
                .where(USERAUTH.SERVICE.equal("password").and(USER.NAME.equal(name)))
                .fetchOne();
            if (record != null) {
                auth = UserAuthDto.fromRecord(record);
            }
        } catch (Exception e) {
            auth = null;
            logger.error("exception", e);
        }
        return auth;
    }

    public UserDto getUserForToken(String token) {
        try {
            Record record = ctx
                .select()
                .from(USERAUTH)
                .join(USER).on(USERAUTH.USER_ID.equal(USER.ID))
                .where(USERAUTH.SERVICE.equal("token").and(USERAUTH.AUTH_KEY.equal(token)))
                .fetchOne();
            if (record != null) {
                return UserAuthDto.fromRecord(record).getUser();
            }
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return null;
    }

    private UserAuthDto getUserAuthByExternalId(String service, String externalId) {
        return UserAuthDto.fromRecord(
            ctx
                .select()
                .from(USERAUTH)
                .leftOuterJoin(USER).on(USERAUTH.USER_ID.equal(USER.ID))
                .where(
                    USERAUTH.SERVICE.equal(service),
                    USERAUTH.AUTH_ID.equal(externalId)
                )
                .fetchOne()
        );
    }

    public UserAuthDto createAuthFromProfile(SocialProfile profile, UserDto user) {
        return UserAuthDto.fromRecord(
            ctx
                .insertInto(
                    USERAUTH,
                    USERAUTH.AUTH_NAME,
                    USERAUTH.AUTH_ID,
                    USERAUTH.AUTH_KEY,
                    USERAUTH.SERVICE,
                    USERAUTH.USER_ID
                )
                .values(
                    profile.getName(),
                    profile.getId(),
                    profile.getToken().getToken(),
                    profile.getService(),
                    user.getId()
                )
                .returning()
                .fetchOne(),
            user
        );
    }

    private UserDto createUser(String name, String email, GlobalRole role) {
        return UserDto.fromRecord(
            ctx
                .insertInto(USER, USER.NAME, USER.BANNED, USER.COLOR, USER.RENAME_AVAILABLE, USER.ROLE, USER.EMAIL, USER.EMAIL_VERIFIED)
                .values(name, false, Colors.generateColor(name), false, role.toString(), email, false)
                .returning()
                .fetchOne()
        );
    }

    @Transactional
    public UserAuthDto getOrCreateUserAuth(SocialProfile profile, UserDto user) {
        UserAuthDto result = getUserAuthByExternalId(profile.getService(), profile.getId());
        if (result == null) {
            if (user != null) {
                result = createAuthFromProfile(profile, user);
            }
        } else {
            Map<Field<?>, Object> extras = new HashMap<>();
            if (user != null) {
                extras.put(USERAUTH.USER_ID, user.getId());
            }
            ctx
                .update(USERAUTH)
                .set(USERAUTH.AUTH_NAME, profile.getName())
                .set(USERAUTH.AUTH_KEY, profile.getToken().getToken())
                .set(extras)
                .where(USERAUTH.SERVICE.equal(profile.getService()).and(USERAUTH.AUTH_ID.equal(String.valueOf(profile.getId()))))
                .execute();
        }
        return result;
    }

    @Transactional
    public UserDto registerWithPassword(String name, String password, String email, String verificationCode) {
        UserDto user = createUser(name, email, GlobalRole.USER_UNCONFIRMED);
        if (user != null) {
            ctx
                .insertInto(USERAUTH, USERAUTH.AUTH_NAME, USERAUTH.AUTH_ID, USERAUTH.AUTH_KEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                .values(null, null, password, "password", user.getId())
                .execute();
            ctx
                .insertInto(PENDING_CONFIRMATION, PENDING_CONFIRMATION.CODE, PENDING_CONFIRMATION.USER_ID)
                .values(verificationCode, user.getId())
                .execute();
        }
        return user;
    }

    @Transactional
    public void setEmail(long userId, String email, String verificationCode) {
        try {
            ctx
                .update(USER)
                .set(USER.EMAIL, email)
                .set(USER.EMAIL_VERIFIED, false)
                .where(USER.ID.equal(userId))
                .execute();
            ctx
                .insertInto(PENDING_CONFIRMATION, PENDING_CONFIRMATION.CODE, PENDING_CONFIRMATION.USER_ID)
                .values(verificationCode, userId)
                .onDuplicateKeyUpdate()
                .set(PENDING_CONFIRMATION.CODE, verificationCode)
                .execute();
        } catch (DataAccessException e) {
            throw new BadRequestException("This email is already in use.");
        }
    }

    public void setPassword(long userId, String passwordHash) {
        try {
            ctx
                .insertInto(USERAUTH, USERAUTH.AUTH_NAME, USERAUTH.AUTH_ID, USERAUTH.AUTH_KEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                .values(null, null, passwordHash, "password", userId)
                .onDuplicateKeyUpdate()
                .set(USERAUTH.AUTH_KEY, passwordHash)
                .execute();
        } catch (Exception e) {
            logger.error("exception", e);
        }
    }

    public boolean setToken(long userId, String token) {
        boolean success = false;
        try {
            ctx
                .insertInto(USERAUTH, USERAUTH.AUTH_NAME, USERAUTH.AUTH_ID, USERAUTH.AUTH_KEY, USERAUTH.SERVICE, USERAUTH.USER_ID)
                .values(null, null, token, "token", userId)
                .onDuplicateKeyUpdate()
                .set(USERAUTH.AUTH_KEY, token)
                .execute();
            success = true;
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return success;
    }

    public UserAuthDto getAuthDataForUser(long id, String service) {
        UserAuthDto auth = null;
        try {
            Record record = ctx
                .select(USERAUTH.AUTH_KEY, USERAUTH.AUTH_NAME)
                .from(USERAUTH)
                .join(USER).on(USER.ID.equal(USERAUTH.USER_ID))
                .where(USER.ID.equal(id).and(USERAUTH.SERVICE.equal(service)))
                .fetchOne();
            if (record != null) {
                auth = new UserAuthDto(0, null, null, null, record.getValue(USERAUTH.AUTH_KEY), record.getValue(USERAUTH.AUTH_NAME));
            }
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return auth;
    }


    @Transactional
    public boolean verifyEmail(final String code, long userId) {
        Record record = ctx
            .select()
            .from(PENDING_CONFIRMATION.join(USER).on(PENDING_CONFIRMATION.USER_ID.equal(USER.ID)))
            .where(PENDING_CONFIRMATION.CODE.equal(code).and(USER.ID.equal(userId)))
            .fetchOne();
        if (record != null) {
            PendingConfirmation pendingConfirmation = record.into(PENDING_CONFIRMATION).into(PendingConfirmation.class);
            UserDto user = UserDto.fromRecord(record.into(USER));
            if (user.hasRole(GlobalRole.USER)) {
                ctx
                    .update(USER)
                    .set(USER.EMAIL_VERIFIED, true)
                    .where(USER.ID.equal(pendingConfirmation.getUserId()))
                    .execute();
            } else {
                ctx
                    .update(USER)
                    .set(USER.ROLE, GlobalRole.USER.toString())
                    .set(USER.EMAIL_VERIFIED, true)
                    .where(USER.ID.equal(pendingConfirmation.getUserId()))
                    .execute();
            }
            ctx
                .delete(PENDING_CONFIRMATION)
                .where(PENDING_CONFIRMATION.ID.equal(pendingConfirmation.getId()))
                .execute();
            return true;
        }
        return false;
    }

    public String getPendingVerificationCode(long id) {
        String code = null;
        try {
            code = ctx
                .select(PENDING_CONFIRMATION.CODE)
                .from(PENDING_CONFIRMATION)
                .where(PENDING_CONFIRMATION.USER_ID.equal(id))
                .fetchOne()
                .value1();
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return code;
    }

    @Transactional
    public UserAuthDto createUserWithProfile(String name, SocialProfile profile) {
        return createAuthFromProfile(profile, createUser(name, profile.getEmail(), GlobalRole.USER));
    }

    public void deleteAuth(UserDto user, String serviceName) {
        //excluding token because user can be easily locked out with only token auth
        if (!"token".equals(serviceName)) {
            int authCount = ctx
                .fetchCount(
                    USERAUTH,
                    USERAUTH.USER_ID.equal(user.getId()).and(USERAUTH.SERVICE.notEqual("token"))
                );
            if (authCount <= 1) {
                throw new BadRequestException("You can't delete last auth method.");
            }
        }
        int deleted = ctx
            .delete(USERAUTH)
            .where(
                USERAUTH.USER_ID.equal(user.getId()),
                USERAUTH.SERVICE.equal(serviceName)
            )
            .execute();
        if (deleted != 1) {
            throw new BadRequestException("service not connected");
        }
    }
}
