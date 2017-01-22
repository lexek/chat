package lexek.wschat.db.dao;

import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserDto;
import org.jooq.DSLContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static lexek.wschat.db.jooq.tables.Session.SESSION;
import static lexek.wschat.db.jooq.tables.User.USER;

@Service
public class SessionDao {
    private final DSLContext ctx;

    @Inject
    public SessionDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public SessionDto createSession(String sid, String ip, UserDto user, long timestamp) {
        return SessionDto.fromRecord(
            ctx
                .insertInto(SESSION, SESSION.IP, SESSION.SID, SESSION.USER_ID, SESSION.EXPIRES)
                .values(ip, sid, user.getId(), timestamp + TimeUnit.DAYS.toMillis(30))
                .returning()
                .fetchOne(),
            user
        );
    }

    public SessionDto getSession(String sid, String ip) {
        return SessionDto.fromRecord(
            ctx
                .select()
                .from(SESSION)
                .leftOuterJoin(USER).on(SESSION.USER_ID.equal(USER.ID))
                .where(SESSION.SID.equal(sid)
                    .and(SESSION.IP.equal(ip).or(USER.CHECK_IP.isFalse()))
                    .and(SESSION.EXPIRES.greaterOrEqual(System.currentTimeMillis())))
                .fetchOne()
        );
    }

    public void invalidateSession(String sid) {
        ctx
            .delete(SESSION)
            .where(SESSION.SID.equal(sid))
            .execute();
    }

    public void invalidateUserSessions(long userId) {
        ctx
            .delete(SESSION)
            .where(SESSION.USER_ID.equal(userId))
            .execute();
    }

    public void invalidateUserSessionsExcept(Long userId, String sid) {
        ctx
            .delete(SESSION)
            .where(
                SESSION.USER_ID.equal(userId),
                SESSION.SID.notEqual(sid)
            )
            .execute();
    }
}
