package lexek.wschat.db.dao;

import lexek.wschat.chat.MessageType;
import lexek.wschat.db.jooq.tables.pojos.History;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.HistoryData;
import lexek.wschat.util.Pages;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.History.HISTORY;
import static lexek.wschat.db.jooq.tables.User.USER;

public class HistoryDao {
    private final Logger logger = LoggerFactory.getLogger(HistoryDao.class);
    private final DataSource dataSource;

    public HistoryDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void add(History object) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection).newRecord(HISTORY, object).store();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }

    public void hideUserMessages(History message, String name, long since) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection).transaction(txCfg -> {
                DSL.using(txCfg)
                        .update(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                        .set(HISTORY.HIDDEN, true)
                        .where(USER.NAME.equal(name).and(HISTORY.TIMESTAMP.greaterOrEqual(since)))
                        .execute();
                DSL.using(txCfg).newRecord(HISTORY, message).store();
            });
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }

    public void hideRoomMessages(long roomId, long since) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection).transaction(txCfg -> {
                DSL.using(connection)
                        .update(HISTORY)
                        .set(HISTORY.HIDDEN, true)
                        .where(HISTORY.ROOM_ID.equal(roomId).and(HISTORY.TIMESTAMP.greaterOrEqual(since)))
                        .execute();
            });
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }

    public DataPage<HistoryData> getAll(long roomId, int page, int pageLength) {
        DataPage<HistoryData> result = null;
        try (Connection connection = dataSource.getConnection()) {
            int count = DSL.using(connection).fetchCount(DSL.select().from(HISTORY).where(HISTORY.ROOM_ID.equal(roomId)));
            List<HistoryData> data = DSL.using(connection)
                    .select(HISTORY.MESSAGE, HISTORY.TYPE, HISTORY.TIMESTAMP, USER.NAME, HISTORY.HIDDEN)
                    .from(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                    .where(HISTORY.ROOM_ID.equal(roomId))
                    .orderBy(HISTORY.TIMESTAMP.desc())
                    .limit(pageLength * page, pageLength)
                    .fetch()
                    .stream()
                    .map(record -> new HistoryData(
                            record.getValue(HISTORY.MESSAGE),
                            MessageType.valueOf(record.getValue(HISTORY.TYPE)),
                            record.getValue(HISTORY.TIMESTAMP),
                            record.getValue(USER.NAME),
                            record.getValue(HISTORY.HIDDEN)
                    ))
                    .collect(Collectors.toList());
            result = new DataPage<>(data, page, Pages.pageCount(pageLength, count));
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public DataPage<HistoryData> getAllForUsers(long roomId, int page, int pageLength, List<String> userNames) {
        DataPage<HistoryData> result = null;
        try (Connection connection = dataSource.getConnection()) {
            int count = DSL.using(connection).fetchCount(DSL.select(DSL.one())
                    .from(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                    .where(HISTORY.ROOM_ID.equal(roomId).and(USER.NAME.in(userNames))));
            List<HistoryData> data = DSL.using(connection)
                    .select(HISTORY.MESSAGE, HISTORY.TYPE, HISTORY.TIMESTAMP, USER.NAME, HISTORY.HIDDEN)
                    .from(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                    .where(HISTORY.ROOM_ID.equal(roomId).and(USER.NAME.in(userNames)))
                    .orderBy(HISTORY.TIMESTAMP.desc())
                    .limit(pageLength * page, pageLength)
                    .fetch()
                    .stream()
                    .map(record -> new HistoryData(
                            record.getValue(HISTORY.MESSAGE),
                            MessageType.valueOf(record.getValue(HISTORY.TYPE)),
                            record.getValue(HISTORY.TIMESTAMP),
                            record.getValue(USER.NAME),
                            record.getValue(HISTORY.HIDDEN)
                    ))
                    .collect(Collectors.toList());
            result = new DataPage<>(data, page, Pages.pageCount(pageLength, count));
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public List<HistoryData> getLast20(long roomId) {
        List<HistoryData> result = null;
        try (Connection connection = dataSource.getConnection()) {
            result = DSL.using(connection)
                    .select(HISTORY.MESSAGE, HISTORY.TYPE, HISTORY.TIMESTAMP, USER.NAME, HISTORY.HIDDEN)
                    .from(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                    .where(HISTORY.ROOM_ID.equal(roomId))
                    .orderBy(HISTORY.TIMESTAMP.desc())
                    .limit(20)
                    .fetch()
                    .stream()
                    .map(record -> new HistoryData(
                            record.getValue(HISTORY.MESSAGE),
                            MessageType.valueOf(record.getValue(HISTORY.TYPE)),
                            record.getValue(HISTORY.TIMESTAMP),
                            record.getValue(USER.NAME),
                            record.getValue(HISTORY.HIDDEN)
                    ))
                    .collect(Collectors.toList());
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }
}
