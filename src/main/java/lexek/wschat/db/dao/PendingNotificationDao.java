package lexek.wschat.db.dao;

import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.jooq.tables.records.PendingNotificationRecord;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static lexek.wschat.db.jooq.tables.PendingNotification.PENDING_NOTIFICATION;

@Service
public class PendingNotificationDao {
    private final DataSource dataSource;

    @Inject
    public PendingNotificationDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void add(long userId, String message) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection).executeInsert(new PendingNotificationRecord(null, userId, message));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public List<String> getPendingNotifications(long userId) {
        List<String> result;
        try (Connection connection = dataSource.getConnection()) {
            result = DSL.using(connection).transactionResult(txCtx -> {
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

        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
    }
}
