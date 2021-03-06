package lexek.wschat.db.dao;

import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.model.ChatterData;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.tx.Transactional;
import lexek.wschat.util.Pages;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.Chatter.CHATTER;
import static lexek.wschat.db.jooq.tables.User.USER;

@Service
public class ChatterDao {
    private final DSLContext ctx;

    @Inject
    public ChatterDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Transactional
    public Chatter getChatter(User user, long roomId) {
        Chatter chatter = Chatter.fromRecord(
            ctx
                .select()
                .from(CHATTER)
                .where(CHATTER.USER_ID.equal(user.getId()).and(CHATTER.ROOM_ID.equal(roomId)))
                .fetchOne(),
            user
        );
        if (chatter == null) {
            long id = ctx
                .insertInto(CHATTER, CHATTER.USER_ID, CHATTER.ROOM_ID, CHATTER.ROLE, CHATTER.TIMEOUT, CHATTER.BANNED)
                .values(user.getId(), roomId, LocalRole.USER.toString(), null, false)
                .returning(CHATTER.ID)
                .fetchOne().getId();
            chatter = new Chatter(id, LocalRole.USER, false, null, user);
        }
        return chatter;
    }

    public Chatter getChatter(String name, long roomId) {
        Chatter chatter = null;
        Record record = ctx
            .select()
            .from(CHATTER.join(USER).on(CHATTER.USER_ID.equal(USER.ID)))
            .where(USER.NAME.equal(name).and(CHATTER.ROOM_ID.equal(roomId)))
            .fetchOne();
        if (record != null) {
            chatter = Chatter.fromRecord(record, new User(UserDto.fromRecord(record)));
        }
        return chatter;
    }

    public void banChatter(long chatterId) {
        ctx
            .update(CHATTER)
            .set(CHATTER.BANNED, true)
            .where(CHATTER.ID.equal(chatterId))
            .execute();
    }

    public void unbanChatter(long chatterId) {
        ctx
            .update(CHATTER)
            .set(CHATTER.BANNED, false)
            .set(CHATTER.TIMEOUT, (Long) null)
            .where(CHATTER.ID.equal(chatterId))
            .execute();
    }

    public void setTimeout(long chatterId, Long until) {
        ctx
            .update(CHATTER)
            .set(CHATTER.TIMEOUT, until)
            .where(CHATTER.ID.equal(chatterId))
            .execute();
    }

    public void setRole(long chatterId, LocalRole newRole) {
        ctx
            .update(CHATTER)
            .set(CHATTER.ROLE, newRole.toString())
            .where(CHATTER.ID.equal(chatterId))
            .execute();
    }

    public DataPage<ChatterData> getAllPaged(long room, int page, int pageLength) {
        DataPage<ChatterData> result;
        List<ChatterData> data = ctx
            .select(
                CHATTER.ID, CHATTER.USER_ID, CHATTER.ROLE, CHATTER.TIMEOUT, CHATTER.BANNED,
                USER.NAME, USER.ROLE
            )
            .from(CHATTER.join(USER).on(CHATTER.USER_ID.equal(USER.ID)))
            .where(CHATTER.ROOM_ID.equal(room))
            .orderBy(CHATTER.ID)
            .limit(page * pageLength, pageLength)
            .fetch()
            .stream()
            .map(record -> new ChatterData(
                record.getValue(CHATTER.ID),
                record.getValue(CHATTER.USER_ID),
                record.getValue(USER.NAME),
                LocalRole.valueOf(record.getValue(CHATTER.ROLE)),
                GlobalRole.valueOf(record.getValue(USER.ROLE)),
                record.getValue(CHATTER.TIMEOUT) != null,
                record.getValue(CHATTER.BANNED)
            ))
            .collect(Collectors.toList());
        result = new DataPage<>(data, page, Pages.pageCount(pageLength, count(room, null)));
        return result;
    }

    public DataPage<ChatterData> searchPaged(long room, int page, int pageLength, String nameParam) {
        List<ChatterData> data = ctx
            .select(
                CHATTER.ID, CHATTER.USER_ID, CHATTER.ROLE, CHATTER.TIMEOUT, CHATTER.BANNED,
                USER.NAME, USER.ROLE
            )
            .from(CHATTER.join(USER).on(CHATTER.USER_ID.equal(USER.ID)))
            .where(CHATTER.ROOM_ID.equal(room).and(USER.NAME.like(nameParam, '!')))
            .orderBy(CHATTER.ID)
            .limit(page * pageLength, pageLength)
            .fetch()
            .stream()
            .map(record -> new ChatterData(
                record.getValue(CHATTER.ID),
                record.getValue(CHATTER.USER_ID),
                record.getValue(USER.NAME),
                LocalRole.valueOf(record.getValue(CHATTER.ROLE)),
                GlobalRole.valueOf(record.getValue(USER.ROLE)),
                record.getValue(CHATTER.TIMEOUT) != null,
                record.getValue(CHATTER.BANNED)
            ))
            .collect(Collectors.toList());
        int count = count(room, USER.NAME.like(nameParam, '!'));
        return new DataPage<>(data, page, Pages.pageCount(pageLength, count));
    }

    public DataPage<ChatterData> getBanned(long room, int page, int pageLength) {
        List<ChatterData> data = ctx
            .select(
                CHATTER.ID, CHATTER.USER_ID, CHATTER.ROLE, CHATTER.TIMEOUT, CHATTER.BANNED,
                USER.NAME, USER.ROLE
            )
            .from(CHATTER.join(USER).on(CHATTER.USER_ID.equal(USER.ID)))
            .where(CHATTER.ROOM_ID.equal(room).and(CHATTER.BANNED.equal(true)))
            .orderBy(CHATTER.ID)
            .limit(page * pageLength, pageLength)
            .fetch()
            .stream()
            .map(record -> new ChatterData(
                record.getValue(CHATTER.ID),
                record.getValue(CHATTER.USER_ID),
                record.getValue(USER.NAME),
                LocalRole.valueOf(record.getValue(CHATTER.ROLE)),
                GlobalRole.valueOf(record.getValue(USER.ROLE)),
                record.getValue(CHATTER.TIMEOUT) != null,
                record.getValue(CHATTER.BANNED)
            ))
            .collect(Collectors.toList());
        int count = count(room, CHATTER.BANNED.equal(true));
        return new DataPage<>(data, page, Pages.pageCount(pageLength, count));
    }

    private int count(long room, Condition condition) {
        if (condition != null) {
            return ctx.fetchCount(
                CHATTER.join(USER).on(CHATTER.USER_ID.equal(USER.ID)),
                CHATTER.ROOM_ID.eq(room).and(condition)
            );
        } else {
            return ctx.fetchCount(CHATTER, CHATTER.ROOM_ID.eq(room));
        }
    }
}
