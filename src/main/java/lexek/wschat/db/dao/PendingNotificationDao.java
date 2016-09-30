package lexek.wschat.db.dao;

import lexek.wschat.db.jooq.tables.records.PendingNotificationRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

import static lexek.wschat.db.jooq.tables.PendingNotification.PENDING_NOTIFICATION;

@Service
public class PendingNotificationDao {
    private final DSLContext ctx;

    @Inject
    public PendingNotificationDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public void add(long userId, String message) {
        ctx.executeInsert(new PendingNotificationRecord(null, userId, message));
    }

    public List<String> getPendingNotifications(long userId) {
        return ctx.transactionResult(txCtx -> {
            List<String> r = DSL.using(txCtx)
                .select(PENDING_NOTIFICATION.TEXT)
                .from(PENDING_NOTIFICATION)
                .where(PENDING_NOTIFICATION.USER_ID.equal(userId))
                .fetch(PENDING_NOTIFICATION.TEXT);
            if (r != null && r.size() > 0) {
                DSL.using(txCtx)
                    .deleteFrom(PENDING_NOTIFICATION)
                    .where(PENDING_NOTIFICATION.USER_ID.equal(userId))
                    .execute();
            }
            return r;
        });
    }
}
