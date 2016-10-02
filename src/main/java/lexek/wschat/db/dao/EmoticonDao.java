package lexek.wschat.db.dao;

import lexek.wschat.db.model.Emoticon;
import org.jooq.DSLContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

import static lexek.wschat.db.jooq.tables.Emoticon.EMOTICON;

@Service
public class EmoticonDao {
    private final DSLContext ctx;

    @Inject
    public EmoticonDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public void addEmoticon(Emoticon emoticon) {
        ctx
            .insertInto(EMOTICON, EMOTICON.CODE, EMOTICON.FILE_NAME, EMOTICON.WIDTH, EMOTICON.HEIGHT)
            .values(emoticon.getCode(), emoticon.getFileName(), emoticon.getWidth(), emoticon.getHeight())
            .execute();
    }

    public Emoticon delete(long id) {
        Emoticon emoticon = ctx
            .select()
            .from(EMOTICON)
            .where(EMOTICON.ID.equal(id))
            .fetchOneInto(Emoticon.class);
        ctx
            .delete(EMOTICON)
            .where(EMOTICON.ID.equal(id))
            .execute();
        return emoticon;
    }

    public List<Emoticon> getAll() {
        return ctx
            .select()
            .from(EMOTICON)
            .orderBy(EMOTICON.CODE.desc())
            .fetchInto(Emoticon.class);
    }

    public void changeFile(Long id, String fileName, int width, int height) {
        ctx
            .update(EMOTICON)
            .set(EMOTICON.FILE_NAME, fileName)
            .set(EMOTICON.WIDTH, width)
            .set(EMOTICON.HEIGHT, height)
            .where(EMOTICON.ID.equal(id))
            .execute();
    }
}
