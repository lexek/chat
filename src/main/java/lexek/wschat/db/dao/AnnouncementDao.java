package lexek.wschat.db.dao;

import lexek.wschat.db.jooq.tables.pojos.Announcement;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

import static lexek.wschat.db.jooq.tables.Announcement.ANNOUNCEMENT;

@Service
public class AnnouncementDao {
    private final DSLContext ctx;

    @Inject
    public AnnouncementDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public void add(Announcement pojo) {
        Record r = ctx
            .insertInto(ANNOUNCEMENT, ANNOUNCEMENT.ROOM_ID, ANNOUNCEMENT.ACTIVE, ANNOUNCEMENT.TEXT)
            .values(pojo.getRoomId(), true, pojo.getText())
            .returning(ANNOUNCEMENT.ID)
            .fetchOne();
        pojo.setId(r.getValue(ANNOUNCEMENT.ID));
    }

    public List<Announcement> getAll() {
        return ctx
            .select()
            .from(ANNOUNCEMENT)
            .where(ANNOUNCEMENT.ACTIVE.isTrue())
            .fetch().into(Announcement.class);
    }

    public void setInactive(long id) {
        ctx
            .update(ANNOUNCEMENT)
            .set(ANNOUNCEMENT.ACTIVE, false)
            .where(ANNOUNCEMENT.ID.equal(id))
            .execute();
    }
}
