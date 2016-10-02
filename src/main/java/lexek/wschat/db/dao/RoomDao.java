package lexek.wschat.db.dao;

import lexek.wschat.db.jooq.tables.pojos.Room;
import org.jooq.DSLContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

import static lexek.wschat.db.jooq.tables.Room.ROOM;

@Service
public class RoomDao {
    private final DSLContext ctx;

    @Inject
    public RoomDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public void add(Room room) {
        long id = ctx
            .insertInto(ROOM, ROOM.NAME, ROOM.TOPIC)
            .values(room.getName(), room.getTopic())
            .returning(ROOM.ID)
            .fetchOne().getId();
        room.setId(id);
    }

    public List<Room> getAll() {
        return ctx
            .select()
            .from(ROOM)
            .fetch()
            .into(Room.class);
    }

    public void delete(long id) {
        ctx
            .delete(ROOM)
            .where(ROOM.ID.equal(id))
            .execute();
    }

    public void updateTopic(long roomId, String newTopic) {
        ctx
            .update(ROOM)
            .set(ROOM.TOPIC, newTopic)
            .where(ROOM.ID.equal(roomId))
            .execute();
    }
}
