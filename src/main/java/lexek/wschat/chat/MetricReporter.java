package lexek.wschat.chat;

import com.codahale.metrics.*;
import lexek.wschat.db.jooq.tables.records.MetricRecord;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;


public class MetricReporter extends ScheduledReporter {
    private final DataSource dataSource;
    private final Logger logger = LoggerFactory.getLogger(MetricReporter.class);

    public MetricReporter(MetricRegistry registry, String name, DataSource dataSource) {
        super(registry, name, MetricFilter.ALL, TimeUnit.MINUTES, TimeUnit.MILLISECONDS);
        this.dataSource = dataSource;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
        try {
            long time = System.currentTimeMillis();
            Map<String, Long> onlineCount = (Map<String, Long>) gauges.get("online").getValue();

            List<MetricRecord> records = new ArrayList<>();

            records.add(new MetricRecord(null, "online", time, Double.valueOf(onlineCount.get("online"))));
            records.add(new MetricRecord(null, "online.authenticated", time, Double.valueOf(onlineCount.get("authenticated"))));
            records.add(new MetricRecord(null, "online.active", time, Double.valueOf(onlineCount.get("active"))));

            try (java.sql.Connection connection = dataSource.getConnection()) {
                DSL.using(connection)
                    .batchInsert(records)
                    .execute();
            } catch (DataAccessException | SQLException e) {
                logger.warn("sql exception", e);
            }
        } catch (Exception e) {
            logger.error("Exception while submitting metric values.", e);
        }
    }

    public void start() {
        start(5, TimeUnit.MINUTES);
    }
}
