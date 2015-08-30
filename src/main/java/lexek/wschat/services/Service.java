package lexek.wschat.services;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;

public interface Service {
    String getName();

    void start() throws Exception;

    void stop();

    void registerMetrics(MetricRegistry metricRegistry);

    HealthCheck getHealthCheck();
}
