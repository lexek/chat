package lexek.wschat.db.dao;

import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.model.Emoticon;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static lexek.wschat.db.jooq.tables.Emoticon.EMOTICON;

public class EmoticonDao {
    private final DataSource dataSource;

    public EmoticonDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void addEmoticon(Emoticon emoticon) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            ctx
                .insertInto(EMOTICON, EMOTICON.CODE, EMOTICON.FILE_NAME, EMOTICON.WIDTH, EMOTICON.HEIGHT)
                .values(emoticon.getCode(), emoticon.getFileName(), emoticon.getWidth(), emoticon.getHeight())
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public Emoticon delete(long id) {
        Emoticon emoticon;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            emoticon = ctx
                .select()
                .from(EMOTICON)
                .where(EMOTICON.ID.equal(id))
                .fetchOneInto(Emoticon.class);
            ctx
                .delete(EMOTICON)
                .where(EMOTICON.ID.equal(id))
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return emoticon;
    }

    public List<Emoticon> getAll() {
        try (Connection connection = dataSource.getConnection()) {
            return DSL.using(connection)
                .select()
                .from(EMOTICON)
                .orderBy(EMOTICON.CODE.desc())
                .fetchInto(Emoticon.class);
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public void changeFile(Long id, String fileName, int width, int height) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .update(EMOTICON)
                .set(EMOTICON.FILE_NAME, fileName)
                .set(EMOTICON.WIDTH, width)
                .set(EMOTICON.HEIGHT, height)
                .where(EMOTICON.ID.equal(id))
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }
}
