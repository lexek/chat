package lexek.wschat.db.dao;

import com.google.common.collect.ImmutableList;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.History.HISTORY;

public class StatisticsDao {
    private final Logger logger = LoggerFactory.getLogger(StatisticsDao.class);
    private final DataSource dataSource;

    public StatisticsDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<Long, Long> getUserActivity(long userId) {
        Map<Long, Long> result = null;
        try (Connection connection = dataSource.getConnection()) {
            Table<Record1<Date>> tempTable = DSL
                    .select(DSL.function("FROM_UNIXTIME", Date.class, HISTORY.TIMESTAMP.div(1000)).as("date"))
                    .from(HISTORY)
                    .where(ImmutableList.of(
                            HISTORY.USER_ID.equal(userId),
                            HISTORY.TIMESTAMP.greaterOrEqual(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))
                    ))
                    .asTable("t", "date");

            result = DSL.using(connection)
                    .select(DSL.date(tempTable.field("date", Date.class)).as("d"),
                            DSL.hour(tempTable.field("date", Date.class)).as("h"),
                            DSL.count().as("count"))
                    .from(tempTable)
                    .groupBy(DSL.field("d"), DSL.field("h"))
                    .orderBy(DSL.field("d"), DSL.field("h"))
                    .fetch()
                    .stream()
                    .collect(Collectors.groupingBy(
                            r -> r.value1().getTime() + TimeUnit.HOURS.toMillis(r.value2()),
                            Collectors.summingLong(Record3::value3)
                    ));
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }
}
