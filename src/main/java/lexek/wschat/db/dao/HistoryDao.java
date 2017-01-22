package lexek.wschat.db.dao;

import com.google.common.collect.Multiset;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.db.jooq.tables.pojos.History;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.HistoryData;
import lexek.wschat.db.tx.Transactional;
import lexek.wschat.util.Pages;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.EmoticonUsage.EMOTICON_USAGE;
import static lexek.wschat.db.jooq.tables.History.HISTORY;
import static lexek.wschat.db.jooq.tables.User.USER;

@Service
public class HistoryDao {
    private final DSLContext ctx;

    @Inject
    public HistoryDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public void add(History object) {
        ctx.newRecord(HISTORY, object).store();
    }

    @Transactional
    public void addWithStats(History history, Multiset<Long> stats) {
        ctx.newRecord(HISTORY, history).store();
        List<Query> toStore = new ArrayList<>();
        for (Multiset.Entry<Long> entry : stats.entrySet()) {
            toStore.add(DSL
                .insertInto(EMOTICON_USAGE)
                .set(EMOTICON_USAGE.EMOTICON_ID, entry.getElement())
                .set(EMOTICON_USAGE.USER_ID, history.getUserId())
                .set(EMOTICON_USAGE.COUNT, (long) entry.getCount())
                .set(EMOTICON_USAGE.DATE, new Date(System.currentTimeMillis()))
                .onDuplicateKeyUpdate()
                .set(EMOTICON_USAGE.COUNT, EMOTICON_USAGE.COUNT.plus(entry.getCount()))
            );
        }
        ctx.batch(toStore).execute();
    }

    @Transactional
    public void hideUserMessages(History message, String name, long since) {
        ctx
            .update(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
            .set(HISTORY.HIDDEN, true)
            .where(USER.NAME.equal(name).and(HISTORY.TIMESTAMP.greaterOrEqual(since)))
            .execute();
        ctx.newRecord(HISTORY, message).store();
    }

    public void hideRoomMessages(long roomId, long since) {
        ctx
            .update(HISTORY)
            .set(HISTORY.HIDDEN, true)
            .where(HISTORY.ROOM_ID.equal(roomId).and(HISTORY.TIMESTAMP.greaterOrEqual(since)))
            .execute();
    }

    public DataPage<HistoryData> getAllForUsers(
        long roomId, int page, int pageLength,
        Optional<List<Long>> users, Optional<Long> since, Optional<Long> until
    ) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(HISTORY.ROOM_ID.equal(roomId));
        users.ifPresent(value -> conditions.add(HISTORY.USER_ID.in(value)));
        since.ifPresent(value -> conditions.add(HISTORY.TIMESTAMP.greaterOrEqual(value)));
        until.ifPresent(value -> conditions.add(HISTORY.TIMESTAMP.lessOrEqual(value)));
        int count = ctx.fetchCount(
            HISTORY,
            DSL.condition(Operator.AND, conditions)
        );
        Table<?> h = DSL
            .select(HISTORY.ID.as("ID"))
            .from(HISTORY)
            .where(conditions)
            .orderBy(HISTORY.TIMESTAMP.desc())
            .limit(pageLength * page, pageLength)
            .asTable("h");
        List<HistoryData> data = ctx
            .select(HISTORY.ID, HISTORY.MESSAGE, HISTORY.TYPE, HISTORY.TIMESTAMP, USER.NAME, HISTORY.HIDDEN, HISTORY.LEGACY)
            .from(
                HISTORY
                    .join(h).on(HISTORY.ID.eq(h.field("ID", Long.class)))
                    .join(USER).on(HISTORY.USER_ID.equal(USER.ID))
            )
            .orderBy(HISTORY.TIMESTAMP.desc())
            .fetch()
            .stream()
            .map(record -> new HistoryData(
                record.getValue(HISTORY.ID),
                record.getValue(HISTORY.MESSAGE),
                MessageType.valueOf(record.getValue(HISTORY.TYPE)),
                record.getValue(HISTORY.TIMESTAMP),
                record.getValue(USER.NAME),
                record.getValue(HISTORY.HIDDEN),
                record.getValue(HISTORY.LEGACY)
            ))
            .collect(Collectors.toList());
        return new DataPage<>(data, page, Pages.pageCount(pageLength, count));
    }

    public List<HistoryData> getLastN(long roomId, int count) {
        Table<?> h = DSL
            .select(HISTORY.ID.as("ID"))
            .from(HISTORY)
            .where(HISTORY.ROOM_ID.equal(roomId))
            .orderBy(HISTORY.TIMESTAMP.desc())
            .limit(count)
            .asTable("h");
        return ctx
            .select(HISTORY.ID, HISTORY.MESSAGE, HISTORY.TYPE, HISTORY.TIMESTAMP, USER.NAME, HISTORY.HIDDEN, HISTORY.LEGACY)
            .from(
                HISTORY
                    .join(h).on(HISTORY.ID.eq(h.field("ID", Long.class)))
                    .join(USER).on(HISTORY.USER_ID.equal(USER.ID))
            )
            .orderBy(HISTORY.TIMESTAMP.desc())
            .fetch()
            .stream()
            .map(record -> new HistoryData(
                record.getValue(HISTORY.ID),
                record.getValue(HISTORY.MESSAGE),
                MessageType.valueOf(record.getValue(HISTORY.TYPE)),
                record.getValue(HISTORY.TIMESTAMP),
                record.getValue(USER.NAME),
                record.getValue(HISTORY.HIDDEN),
                record.getValue(HISTORY.LEGACY)
            ))
            .collect(Collectors.toList());
    }
}
