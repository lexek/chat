package lexek.wschat.db.dao;

import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.model.ChatterData;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.util.Pages;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.Chatter.CHATTER;
import static lexek.wschat.db.jooq.tables.User.USER;

public class ChatterDao {
    private final DataSource dataSource;

    public ChatterDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Chatter getChatter(User user, long roomId) {
        Chatter chatter;
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                .select()
                .from(CHATTER)
                .where(CHATTER.USER_ID.equal(user.getId()).and(CHATTER.ROOM_ID.equal(roomId)))
                .fetchOne();
            chatter = Chatter.fromRecord(record, user);
            if (record == null) {
                long id = DSL.using(connection)
                    .insertInto(CHATTER, CHATTER.USER_ID, CHATTER.ROOM_ID, CHATTER.ROLE, CHATTER.TIMEOUT, CHATTER.BANNED)
                    .values(user.getId(), roomId, LocalRole.USER.toString(), null, false)
                    .returning(CHATTER.ID)
                    .fetchOne().getId();
                chatter = new Chatter(id, LocalRole.USER, false, null, user);
            }
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        if (chatter == null) {
            chatter = new Chatter(null, LocalRole.USER, false, null, user);
        }
        return chatter;
    }

    public Chatter getChatter(String name, long roomId) {
        Chatter chatter = null;
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                .select()
                .from(CHATTER.join(USER).on(CHATTER.USER_ID.equal(USER.ID)))
                .where(USER.NAME.equal(name).and(CHATTER.ROOM_ID.equal(roomId)))
                .fetchOne();
            if (record != null) {
                chatter = Chatter.fromRecord(record, new User(UserDto.fromRecord(record)));
            }
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return chatter;
    }

    public void banChatter(long chatterId) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .update(CHATTER)
                .set(CHATTER.BANNED, true)
                .where(CHATTER.ID.equal(chatterId))
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public void unbanChatter(long chatterId) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .update(CHATTER)
                .set(CHATTER.BANNED, false)
                .set(CHATTER.TIMEOUT, (Long) null)
                .where(CHATTER.ID.equal(chatterId))
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public void setTimeout(long chatterId, Long until) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .update(CHATTER)
                .set(CHATTER.TIMEOUT, until)
                .where(CHATTER.ID.equal(chatterId))
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public void setRole(long chatterId, LocalRole newRole) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .update(CHATTER)
                .set(CHATTER.ROLE, newRole.toString())
                .where(CHATTER.ID.equal(chatterId))
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public DataPage<ChatterData> getAllPaged(long room, int page, int pageLength) {
        DataPage<ChatterData> result;
        try (Connection connection = dataSource.getConnection()) {
            List<ChatterData> data = DSL.using(connection)
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
            result = new DataPage<>(data, page, Pages.pageCount(pageLength, count(connection, room, null)));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
    }

    public DataPage<ChatterData> searchPaged(long room, int page, int pageLength, String nameParam) {
        try (Connection connection = dataSource.getConnection()) {
            List<ChatterData> data = DSL.using(connection)
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
            int count = count(connection, room, USER.NAME.like(nameParam, '!'));
            return new DataPage<>(data, page, Pages.pageCount(pageLength, count));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public DataPage<ChatterData> getBanned(long room, int page, int pageLength) {
        try (Connection connection = dataSource.getConnection()) {
            List<ChatterData> data = DSL.using(connection)
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
            int count = count(connection, room, CHATTER.BANNED.equal(true));
            return new DataPage<>(data, page, Pages.pageCount(pageLength, count));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    private int count(Connection connection, long room, Condition condition) {
        if (condition != null) {
            return DSL.using(connection)
                .fetchCount(
                    CHATTER.join(USER).on(CHATTER.USER_ID.equal(USER.ID)),
                    CHATTER.ROOM_ID.eq(room).and(condition)
                );
        } else {
            return DSL.using(connection).fetchCount(CHATTER, CHATTER.ROOM_ID.eq(room));
        }
    }
}
