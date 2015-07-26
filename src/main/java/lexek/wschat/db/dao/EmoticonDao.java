package lexek.wschat.db.dao;

import lexek.wschat.db.jooq.tables.pojos.Emoticon;
import lexek.wschat.db.model.e.InternalErrorException;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static lexek.wschat.db.jooq.tables.Emoticon.EMOTICON;

public class EmoticonDao {
    private final DataSource dataSource;

    public EmoticonDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getAllAsJson() {
        String result;
        try (Connection connection = dataSource.getConnection()) {
            result = DSL.using(connection)
                .select()
                .from(EMOTICON)
                .orderBy(EMOTICON.CODE.desc())
                .fetch().formatJSON();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
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
}
