package lexek.wschat.services;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lmax.disruptor.EventHandler;
import lexek.wschat.chat.*;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static lexek.wschat.chat.Message.Keys;
import static lexek.wschat.db.jooq.tables.History.HISTORY;
import static lexek.wschat.db.jooq.tables.Room.ROOM;
import static lexek.wschat.db.jooq.tables.User.USER;

public class HistoryService implements EventHandler<MessageEvent> {
    private static final Set<MessageType> STORE_TYPES = ImmutableSet.of(
            MessageType.MSG,
            MessageType.ME,
            MessageType.MSG_EXT,
            MessageType.BAN,
            MessageType.TIMEOUT,
            MessageType.CLEAR,
            MessageType.CLEAR_EXT,
            MessageType.LIKE
    );

    private final int maxHistory;
    private final Gson gson = new Gson();
    private final Logger logger = LoggerFactory.getLogger(HistoryService.class);
    private final DataSource dataSource;

    public HistoryService(DataSource dataSource, int maxHistory) {
        this.dataSource = dataSource;
        this.maxHistory = maxHistory;
    }

    public String getAllPagedAsJson(long roomId, int page, int pageLength) {
        String result = null;
        try (java.sql.Connection connection = dataSource.getConnection()) {
            int count = DSL.using(connection)
                    .fetchCount(DSL.select().from(HISTORY).where(HISTORY.ROOM_ID.equal(roomId)));
            Result<? extends Record> data = DSL.using(connection)
                    .select(HISTORY.MESSAGE, HISTORY.TYPE, HISTORY.TIMESTAMP, USER.NAME, HISTORY.HIDDEN)
                    .from(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                    .where(HISTORY.ROOM_ID.equal(roomId))
                    .orderBy(HISTORY.TIMESTAMP.desc())
                    .limit(pageLength * page, pageLength)
                    .fetch();
            String roomName = DSL.using(connection)
                    .select(ROOM.NAME)
                    .from(ROOM)
                    .where(ROOM.ID.equal(roomId))
                    .fetchOne()
                    .value1();
            result = formatPageJson(data, count / pageLength, roomName);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public String getAllPagedAsJson(long roomId, int page, int pageLength, List<String> users) {
        String result = null;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            int count = ctx
                    .select(DSL.count())
                    .from(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                    .where(HISTORY.ROOM_ID.equal(roomId).and(USER.NAME.in(users)))
                    .fetchOne().value1();
            Result<? extends Record> data = ctx
                    .select(HISTORY.MESSAGE, HISTORY.TYPE, HISTORY.TIMESTAMP, HISTORY.USER_ID, USER.NAME, HISTORY.HIDDEN)
                    .from(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                    .where(HISTORY.ROOM_ID.equal(roomId).and(USER.NAME.in(users)))
                    .orderBy(HISTORY.TIMESTAMP.desc())
                    .limit(pageLength * page, pageLength)
                    .fetch();
            String roomName = ctx.select(ROOM.NAME).from(ROOM).where(ROOM.ID.equal(roomId)).fetchOne().value1();
            result = formatPageJson(data, count / pageLength, roomName);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public JsonArray getLast20(long roomId) {
        JsonArray result = null;
        Result<? extends Record> data = null;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            data = ctx
                    .select(HISTORY.MESSAGE, HISTORY.TYPE, HISTORY.TIMESTAMP, USER.NAME, HISTORY.HIDDEN)
                    .from(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                    .where(HISTORY.ROOM_ID.equal(roomId))
                    .orderBy(HISTORY.TIMESTAMP.desc())
                    .limit(20)
                    .fetch();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        if (data != null) {
            result = new JsonArray();
            for (Record record : data) {
                org.jooq.Field<?>[] fields = data.fields();
                JsonObject object = new JsonObject();
                for (int index = 0; index < fields.length; index++) {
                    object.add(fields[index].getName(), gson.toJsonTree(record.getValue(index)));
                }
                result.add(object);
            }
        }
        return result;
    }

    private String formatPageJson(Result<? extends Record> data, int totalPages, String roomName) {
        JsonObject rootObject = new JsonObject();
        JsonArray dataArray = new JsonArray();
        for (Record record : data) {
            org.jooq.Field<?>[] fields = data.fields();
            JsonObject object = new JsonObject();
            for (int index = 0; index < fields.length; index++) {
                object.add(fields[index].getName(), gson.toJsonTree(record.getValue(index)));
            }
            dataArray.add(object);
        }

        rootObject.add("data", dataArray);
        rootObject.addProperty("totalPages", totalPages);
        rootObject.addProperty("roomName", roomName);

        return gson.toJson(rootObject);
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        final Message message = event.getMessage();
        if (event.getBroadcastFilter().getType() == BroadcastFilter.Type.ROOM) {
            final Room room = (Room) event.getBroadcastFilter().getData();
            if (STORE_TYPES.contains(message.getType())) {
                MessageType type = message.getType();
                final Long userId = event.getConnection().getUser().getId();
                if (type == MessageType.MSG || type == MessageType.ME) {
                    try (Connection connection = dataSource.getConnection()) {
                        DSL.using(connection)
                                .insertInto(HISTORY, HISTORY.TIMESTAMP, HISTORY.ROOM_ID, HISTORY.USER_ID, HISTORY.TYPE,
                                        HISTORY.MESSAGE, HISTORY.HIDDEN)
                                .values(message.get(Keys.TIME), room.getId(), userId,
                                        message.getType().toString(), message.get(Keys.TEXT), false)
                                .execute();
                    } catch (DataAccessException | SQLException e) {
                        logger.error("sql exception", e);
                    }
                } else if (type == MessageType.CLEAR || type == MessageType.BAN || type == MessageType.TIMEOUT) {
                    try (Connection connection = dataSource.getConnection()) {
                        DSL.using(connection).transaction(txCfg -> {
                            String name = message.get(Keys.NAME);
                            long t = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
                            DSL.using(txCfg)
                                    .update(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                                    .set(HISTORY.HIDDEN, true)
                                    .where(USER.NAME.equal(name).and(HISTORY.TIMESTAMP.greaterOrEqual(t)))
                                    .execute();
                            DSL.using(txCfg)
                                    .insertInto(HISTORY, HISTORY.TIMESTAMP, HISTORY.ROOM_ID, HISTORY.USER_ID,
                                            HISTORY.TYPE, HISTORY.MESSAGE, HISTORY.HIDDEN)
                                    .values(t, room.getId(), userId, message.getType().toString(), name, false)
                                    .execute();
                        });
                    } catch (DataAccessException | SQLException e) {
                        logger.error("sql exception", e);
                    }
                }
                if (room != null) {
                    List<Message> msgs = room.getHistory();
                    msgs.add(message);
                    if (msgs.size() == maxHistory) {
                        msgs.remove(0);
                    }
                }
            } else if (message.getType() == MessageType.CLEAR_ROOM) {
                room.getHistory().clear();
                try (Connection connection = dataSource.getConnection()) {
                    long t = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
                    DSL.using(connection)
                            .update(HISTORY)
                            .set(HISTORY.HIDDEN, true)
                            .where(HISTORY.TIMESTAMP.greaterOrEqual(t))
                            .execute();
                } catch (DataAccessException | SQLException e) {
                    logger.error("sql exception", e);
                }
            }
        }
    }
}
