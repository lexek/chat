package lexek.wschat.db.dao;

import lexek.wschat.db.jooq.tables.pojos.Announcement;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static lexek.wschat.db.jooq.tables.Announcement.ANNOUNCEMENT;

public class AnnouncementDao {
    private final Logger logger = LoggerFactory.getLogger(AnnouncementDao.class);
    private final DataSource dataSource;

    public AnnouncementDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void add(Announcement pojo) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            Record r = ctx
                    .insertInto(ANNOUNCEMENT, ANNOUNCEMENT.ROOM_ID, ANNOUNCEMENT.ACTIVE, ANNOUNCEMENT.TEXT)
                    .values(pojo.getRoomId(), true, pojo.getText())
                    .returning(ANNOUNCEMENT.ID)
                    .fetchOne();
            pojo.setId(r.getValue(ANNOUNCEMENT.ID));
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }

    public List<Announcement> getAll() {
        List<Announcement> result = null;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            result = ctx
                    .select()
                    .from(ANNOUNCEMENT)
                    .where(ANNOUNCEMENT.ACTIVE.isTrue())
                    .fetch().into(Announcement.class);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public void setInactive(long id) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            ctx
                    .update(ANNOUNCEMENT)
                    .set(ANNOUNCEMENT.ACTIVE, false)
                    .where(ANNOUNCEMENT.ID.equal(id))
                    .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }
}
