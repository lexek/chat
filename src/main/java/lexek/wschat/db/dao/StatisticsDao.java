package lexek.wschat.db.dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lexek.wschat.db.jooq.tables.pojos.Metric;
import lexek.wschat.db.model.Emoticon;
import lexek.wschat.db.model.EmoticonCount;
import lexek.wschat.db.model.UserMessageCount;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.Emoticon.EMOTICON;
import static lexek.wschat.db.jooq.tables.EmoticonUsage.EMOTICON_USAGE;
import static lexek.wschat.db.jooq.tables.History.HISTORY;
import static lexek.wschat.db.jooq.tables.Metric.METRIC;
import static lexek.wschat.db.jooq.tables.User.USER;

@Service
public class StatisticsDao {
    private final DSLContext ctx;

    @Inject
    public StatisticsDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public Map<Long, Long> getUserActivity(long userId) {
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

        return ctx
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
    }

    public Map<Long, Long> getRoomActivity(long roomId) {
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

        return ctx
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
    }

    public List<UserMessageCount> getTopChatters(long roomId) {
        return ctx
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
    }

    public List<Metric> getMetrics(long since) {
        return ctx
            .select(METRIC.NAME, METRIC.TIME, METRIC.VALUE)
            .from(METRIC)
            .where(METRIC.TIME.greaterOrEqual(since))
            .orderBy(METRIC.TIME)
            .fetch()
            .into(Metric.class);
    }

    public List<EmoticonCount> getEmoticonUage(long since, Long userId) {
        List<Condition> where = Lists.newArrayList(
            EMOTICON_USAGE.DATE.greaterOrEqual(new Date(since))
        );
        if (userId != null) {
            where.add(EMOTICON_USAGE.USER_ID.equal(userId));
        }
        return ctx
            .select(
                EMOTICON.ID,
                EMOTICON.FILE_NAME,
                EMOTICON.CODE,
                DSL.sum(EMOTICON_USAGE.COUNT).as("sum")
            )
            .from(
                EMOTICON_USAGE.join(EMOTICON).on(EMOTICON_USAGE.EMOTICON_ID.eq(EMOTICON.ID))
            )
            .where(where)
            .groupBy(EMOTICON_USAGE.EMOTICON_ID)
            .orderBy(DSL.field("sum").desc(), EMOTICON_USAGE.EMOTICON_ID.desc())
            .fetch()
            .stream()
            .map(record ->
                new EmoticonCount(
                    new Emoticon(
                        record.getValue(EMOTICON.ID),
                        record.getValue(EMOTICON.CODE),
                        record.getValue(EMOTICON.FILE_NAME),
                        null,
                        null
                    ),
                    record.getValue(DSL.field("sum", BigDecimal.class)).longValue()
                )
            )
            .collect(Collectors.toList());
    }

    public List<UserMessageCount> getEmoticonUsers(long since, long emoticonId) {
        return ctx
            .select(
                USER.ID,
                USER.NAME,
                DSL.sum(EMOTICON_USAGE.COUNT).as("sum")
            )
            .from(
                EMOTICON_USAGE.join(USER).on(EMOTICON_USAGE.USER_ID.eq(USER.ID))
            )
            .where(
                EMOTICON_USAGE.EMOTICON_ID.eq(emoticonId),
                EMOTICON_USAGE.DATE.greaterOrEqual(new Date(since))
            )
            .groupBy(USER.ID)
            .orderBy(DSL.field("sum", BigDecimal.class).desc(), USER.NAME.asc())
            .fetch()
            .stream()
            .map(record ->
                new UserMessageCount(
                    record.getValue(USER.NAME),
                    record.getValue(USER.ID),
                    record.getValue(DSL.field("sum", BigDecimal.class)).longValue()
                )
            )
            .collect(Collectors.toList());
    }
}
