package lexek.wschat.db.dao;

import com.google.common.collect.ImmutableList;
import lexek.wschat.db.jooq.tables.pojos.Room;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static lexek.wschat.db.jooq.tables.Room.ROOM;


public class RoomDao {
    private final DataSource dataSource;
    private final Logger logger = LoggerFactory.getLogger(RoomDao.class);

    public RoomDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void add(Room room) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            long id = ctx
                .insertInto(ROOM, ROOM.NAME, ROOM.TOPIC)
                .values(room.getName(), room.getTopic())
                .returning(ROOM.ID)
                .fetchOne().getId();
            room.setId(id);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }

    public List<Room> getAll() {
        List<Room> result = ImmutableList.of();
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            result = ctx
                .select()
                .from(ROOM)
                .fetch()
                .into(Room.class);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public void delete(long id) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            ctx
                .delete(ROOM)
                .where(ROOM.ID.equal(id))
                .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }

    @Deprecated
    public void delete(String name) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            ctx
                .delete(ROOM)
                .where(ROOM.NAME.equal(name))
                .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }
}
