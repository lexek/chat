package lexek.wschat.db.dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.jooq.tables.pojos.Metric;
import lexek.wschat.db.model.Emoticon;
import lexek.wschat.db.model.EmoticonCount;
import lexek.wschat.db.model.UserMessageCount;
import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.Emoticon.EMOTICON;
import static lexek.wschat.db.jooq.tables.History.HISTORY;
import static lexek.wschat.db.jooq.tables.Metric.METRIC;
import static lexek.wschat.db.jooq.tables.User.USER;

@Service
public class StatisticsDao {
    private final DataSource dataSource;

    @Inject
    public StatisticsDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<Long, Long> getUserActivity(long userId) {
        Map<Long, Long> result;
        try (Connection connection = dataSource.getConnection()) {
            Table<Record1<Date>> tempTable = DSL
                .select(DSL.function("FROM_UNIXTIME", Date.class, HISTORY.TIMESTAMP.div(1000)).as("date"))
                .from(HISTORY)
                .where(ImmutableList.of(
                    HISTORY.USER_ID.equal(userId),
                    HISTORY.TIMESTAMP.greaterOrEqual(
                        Instant.now()
                            .minus(Duration.ofDays(7))
                            .truncatedTo(ChronoUnit.DAYS)
                            .toEpochMilli())
                ))
                .asTable("t", "date");

            result = DSL.using(connection)
                .select(DSL.date(tempTable.field("date", Date.class)).as("d"),
                    DSL.hour(tempTable.field("date", Date.class)).as("h"),
                    DSL.count().as("count"))
                .from(tempTable)
                .groupBy(DSL.field("d"), DSL.field("h"))
                .orderBy(DSL.field("d"), DSL.field("h"))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                    r -> r.value1().getTime() + TimeUnit.HOURS.toMillis(r.value2()),
                    Collectors.summingLong(Record3::value3)
                ));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
    }

    public Map<Long, Long> getRoomActivity(long roomId) {
        Map<Long, Long> result;
        try (Connection connection = dataSource.getConnection()) {
            Table<Record1<Date>> tempTable = DSL
                .select(DSL.function("FROM_UNIXTIME", Date.class, HISTORY.TIMESTAMP.div(1000)).as("date"))
                .from(HISTORY)
                .where(
                    HISTORY.ROOM_ID.equal(roomId),
                    HISTORY.TIMESTAMP.greaterOrEqual(
                        Instant.now()
                            .minus(Duration.ofDays(7))
                            .truncatedTo(ChronoUnit.DAYS)
                            .toEpochMilli())
                )
                .asTable("t", "date");

            result = DSL.using(connection)
                .select(
                    DSL.date(tempTable.field("date", Date.class)).as("d"),
                    DSL.hour(tempTable.field("date", Date.class)).as("h"),
                    DSL.count().as("count")
                )
                .from(tempTable)
                .groupBy(DSL.field("d"), DSL.field("h"))
                .orderBy(DSL.field("d"), DSL.field("h"))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                    r -> r.value1().getTime() + TimeUnit.HOURS.toMillis(r.value2()),
                    Collectors.summingLong(Record3::value3)
                ));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
    }

    public List<UserMessageCount> getTopChatters(long roomId) {
        List<UserMessageCount> result;
        try (Connection connection = dataSource.getConnection()) {
            result = DSL.using(connection)
                .select(USER.ID, USER.NAME, DSL.count().as("count"))
                .from(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                .where(ImmutableList.of(
                    HISTORY.ROOM_ID.equal(roomId),
                    HISTORY.TIMESTAMP.greaterOrEqual(Instant.now()
                        .minus(Duration.ofDays(7))
                        .truncatedTo(ChronoUnit.DAYS)
                        .toEpochMilli())
                ))
                .groupBy(HISTORY.USER_ID)
                .orderBy(DSL.field("count", Long.class).desc())
                .limit(20)
                .fetch()
                .stream()
                .map(r -> new UserMessageCount(r.value2(), r.value1(), r.value3()))
                .collect(Collectors.toList());
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
    }

    public List<Metric> getMetrics(long since) {
        List<Metric> metrics;
        try (Connection connection = dataSource.getConnection()) {
            metrics = DSL.using(connection)
                .select(METRIC.NAME, METRIC.TIME, METRIC.VALUE)
                .from(METRIC)
                .where(METRIC.TIME.greaterOrEqual(since))
                .orderBy(METRIC.TIME)
                .fetch()
                .into(Metric.class);
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return metrics;
    }

    public List<EmoticonCount> getEmoticonUage(long since, Long userId) {
        Table<Record4<String, Long, String, String>> emoteTable = DSL.table(
            DSL.select(
                DSL.concat("%", EMOTICON.CODE.replace("\\", "\\\\").replace("_", "\\_")).concat("%").as("code"),
                EMOTICON.ID.as("id"),
                EMOTICON.FILE_NAME.as("file_name"),
                EMOTICON.CODE.as("original_code")
            ).from(EMOTICON)
        ).as("emote");
        List<Condition> where = Lists.newArrayList(
            HISTORY.MESSAGE.like(emoteTable.field("code", String.class)),
            HISTORY.TIMESTAMP.greaterOrEqual(since)
        );
        if (userId != null) {
            where.add(HISTORY.USER_ID.equal(userId));
        }
        try (Connection connection = dataSource.getConnection()) {
            return DSL.using(connection)
                .select(
                    emoteTable.field("id"),
                    emoteTable.field("file_name"),
                    emoteTable.field("original_code"),
                    DSL.count().as("count")
                )
                .from(emoteTable, HISTORY)
                .where(where)
                .groupBy(emoteTable.field("id"))
                .orderBy(DSL.field("count").desc(), emoteTable.field("id").asc())
                .fetch()
                .stream()
                .map(record ->
                    new EmoticonCount(
                        new Emoticon(
                            record.getValue("id", Long.class),
                            record.getValue("original_code", String.class),
                            record.getValue("file_name", String.class),
                            null,
                            null
                        ),
                        record.getValue("count", Long.class)
                    )
                )
                .collect(Collectors.toList());
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public List<UserMessageCount> getEmoticonUsers(long since, long emoticonId) {
        Table<Record4<String, Long, String, String>> emoteTable = DSL.table(DSL
            .select(
                DSL.concat("%", EMOTICON.CODE.replace("\\", "\\\\").replace("_", "\\_")).concat("%").as("code"),
                EMOTICON.ID.as("id"),
                EMOTICON.FILE_NAME.as("file_name"),
                EMOTICON.CODE.as("original_code")
            )
            .from(EMOTICON)
            .where(EMOTICON.ID.equal(emoticonId))
        ).as("emote");
        try (Connection connection = dataSource.getConnection()) {
            return DSL.using(connection)
                .select(
                    USER.ID,
                    USER.NAME,
                    DSL.count().as("count")
                )
                .from(
                    emoteTable,
                    HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID))
                )
                .where(
                    HISTORY.MESSAGE.like(emoteTable.field("code", String.class)),
                    HISTORY.TIMESTAMP.greaterOrEqual(since)
                )
                .groupBy(USER.ID)
                .orderBy(DSL.field("count").desc(), USER.NAME.asc())
                .fetch()
                .stream()
                .map(record ->
                    new UserMessageCount(
                        record.getValue(USER.NAME),
                        record.getValue(USER.ID),
                        record.getValue("count", Long.class)
                    )
                )
                .collect(Collectors.toList());
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }
}
