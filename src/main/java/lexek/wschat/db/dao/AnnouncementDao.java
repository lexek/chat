package lexek.wschat.db.dao;

import lexek.wschat.db.jooq.tables.pojos.Announcement;
import lexek.wschat.db.model.e.InternalErrorException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static lexek.wschat.db.jooq.tables.Announcement.ANNOUNCEMENT;

public class AnnouncementDao {
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
            throw new InternalErrorException(e);
        }
    }

    public List<Announcement> getAll() {
        List<Announcement> result;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            result = ctx
                .select()
                .from(ANNOUNCEMENT)
                .where(ANNOUNCEMENT.ACTIVE.isTrue())
                .fetch().into(Announcement.class);
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
    }

    public void setInactive(long id) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .update(ANNOUNCEMENT)
                .set(ANNOUNCEMENT.ACTIVE, false)
                .where(ANNOUNCEMENT.ID.equal(id))
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }
}
