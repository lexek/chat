package lexek.wschat.chat;

import com.codahale.metrics.*;
import com.codahale.metrics.health.HealthCheck;
import lexek.wschat.db.jooq.tables.records.MetricRecord;
import lexek.wschat.services.managed.InitStage;
import lexek.wschat.services.managed.ManagedService;
import org.jooq.DSLContext;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

@Service
public class MetricReporter extends ScheduledReporter implements ManagedService {
    private final DSLContext ctx;
    private final Logger logger = LoggerFactory.getLogger(MetricReporter.class);

    @Inject
    public MetricReporter(@Named("chatRegistry") MetricRegistry registry, DSLContext ctx) {
        super(registry, "reporter", MetricFilter.ALL, TimeUnit.MINUTES, TimeUnit.MILLISECONDS);
        this.ctx = ctx;
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

            ctx.batchInsert(records).execute();
        } catch (Exception e) {
            logger.error("Exception while submitting metric values.", e);
        }
    }

    @Override
    public void start() {
        start(5, TimeUnit.MINUTES);
    }

    @Override
    public String getName() {
        return "metricReporter";
    }

    @Override
    public void registerMetrics(MetricRegistry metricRegistry) {
        //ignore
    }

    @Override
    public HealthCheck getHealthCheck() {
        return new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy();
            }
        };
    }

    @Override
    public InitStage getStage() {
        return InitStage.SERVICES;
    }
}
