package lexek.wschat.db.dao;

import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.jooq.tables.pojos.Room;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static lexek.wschat.db.jooq.tables.Room.ROOM;


public class RoomDao {
    private final DataSource dataSource;

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
            throw new InternalErrorException(e);
        }
    }

    public List<Room> getAll() {
        List<Room> result;
        try (Connection connection = dataSource.getConnection()) {
            result = DSL.using(connection)
                .select()
                .from(ROOM)
                .fetch()
                .into(Room.class);
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
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
            throw new InternalErrorException(e);
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
            throw new InternalErrorException(e);
        }
    }
}
