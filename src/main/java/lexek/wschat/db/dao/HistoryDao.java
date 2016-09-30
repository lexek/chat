package lexek.wschat.db.dao;

import lexek.wschat.chat.model.MessageType;
import lexek.wschat.db.jooq.tables.pojos.History;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.HistoryData;
import lexek.wschat.util.Pages;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Operator;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public void hideUserMessages(History message, String name, long since) {
        ctx.transaction(txCfg -> {
            DSL.using(txCfg)
                .update(HISTORY.join(USER).on(HISTORY.USER_ID.equal(USER.ID)))
                .set(HISTORY.HIDDEN, true)
                .where(USER.NAME.equal(name).and(HISTORY.TIMESTAMP.greaterOrEqual(since)))
                .execute();
            DSL.using(txCfg).newRecord(HISTORY, message).store();
        });
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
            .select(HISTORY.MESSAGE, HISTORY.TYPE, HISTORY.TIMESTAMP, USER.NAME, HISTORY.HIDDEN)
            .from(
                HISTORY
                    .join(h).on(HISTORY.ID.eq(h.field("ID", Long.class)))
                    .join(USER).on(HISTORY.USER_ID.equal(USER.ID))
            )
            .orderBy(HISTORY.TIMESTAMP.desc())
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
            .select(HISTORY.MESSAGE, HISTORY.TYPE, HISTORY.TIMESTAMP, USER.NAME, HISTORY.HIDDEN)
            .from(
                HISTORY
                    .join(h).on(HISTORY.ID.eq(h.field("ID", Long.class)))
                    .join(USER).on(HISTORY.USER_ID.equal(USER.ID))
            )
            .orderBy(HISTORY.TIMESTAMP.desc())
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
    }
}
