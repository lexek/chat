package lexek.wschat.db.dao;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lexek.wschat.db.jooq.tables.pojos.Journal;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static lexek.wschat.db.jooq.tables.Journal.JOURNAL;

public class JournalDao {
    private final DataSource dataSource;
    private final Gson gson = new Gson();
    private final Logger logger = LoggerFactory.getLogger(JournalDao.class);

    public JournalDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Deprecated
    public static void insert(DSLContext ctx, final Journal pojo) {
        ctx
                .insertInto(JOURNAL, JOURNAL.TIMESTAMP, JOURNAL.MESSAGE, JOURNAL.TAG, JOURNAL.USER)
                .values(pojo.getTimestamp(), pojo.getMessage(), pojo.getTag(), pojo.getUser())
                .execute();
    }

    public void add(Journal journal) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection).newRecord(JOURNAL, journal).store();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }

    public String getAllPagedAsJson(int page, int pageLength) {
        String result = null;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            Result<Record> data = ctx
                    .select()
                    .from(JOURNAL)
                    .orderBy(JOURNAL.TIMESTAMP.desc())
                    .limit(page * pageLength, pageLength)
                    .fetch();
            int total = ctx.fetchCount(ctx.select().from(JOURNAL)) / pageLength;
            result = formatPageJson(data, total);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    private String formatPageJson(Result<Record> data, int totalPages) {
        JsonObject rootObject = new JsonObject();
        JsonArray dataArray = new JsonArray();
        for (Record record : data) {
            org.jooq.Field<?>[] fields = data.fields();
            JsonObject object = new JsonObject();
            for (int index = 0; index < fields.length; index++) {
                object.add(fields[index].getName(), gson.toJsonTree(record.getValue(index)));
            }
            dataArray.add(object);
        }

        rootObject.add("data", dataArray);
        rootObject.addProperty("totalPages", totalPages);

        return gson.toJson(rootObject);
    }
}
