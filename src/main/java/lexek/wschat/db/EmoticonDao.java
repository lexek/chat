package lexek.wschat.db;

import lexek.wschat.db.jooq.tables.pojos.Emoticon;
import lexek.wschat.db.jooq.tables.pojos.Journal;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static lexek.wschat.db.jooq.tables.Emoticon.EMOTICON;

public class EmoticonDao {
    private final DataSource dataSource;
    private final Logger logger = LoggerFactory.getLogger(EmoticonDao.class);

    public EmoticonDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getAllAsJson() {
        String result = null;
        try (Connection connection = dataSource.getConnection()) {
            result = DSL.using(connection)
                    .select()
                    .from(EMOTICON)
                    .orderBy(EMOTICON.CODE.desc())
                    .fetch().formatJSON();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public void addEmoticon(Emoticon emoticon, Journal journalMessage) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            ctx
                    .insertInto(EMOTICON, EMOTICON.CODE, EMOTICON.FILE_NAME, EMOTICON.WIDTH, EMOTICON.HEIGHT)
                    .values(emoticon.getCode(), emoticon.getFileName(), emoticon.getWidth(), emoticon.getHeight())
                    .execute();
            JournalDao.insert(ctx, journalMessage);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }

    public void delete(long id, String user) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            Record result = ctx
                    .select(EMOTICON.CODE)
                    .from(EMOTICON)
                    .where(EMOTICON.ID.equal(id))
                    .fetchOne();
            ctx
                    .delete(EMOTICON)
                    .where(EMOTICON.ID.equal(id))
                    .execute();
            Journal journalMessage = new Journal(null, System.currentTimeMillis(),
                    "Deleted emoticon \"" + result.getValue(EMOTICON.CODE) + "\".", user, "admin");
            JournalDao.insert(ctx, journalMessage);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }
}
