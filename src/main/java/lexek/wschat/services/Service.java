package lexek.wschat.services;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;

import java.util.List;

public interface Service<T> {
    String getName();

    List<String> getAvailableActions();

    void performAction(String action);

    void start() throws Exception;

    void stop();

    void registerMetrics(MetricRegistry metricRegistry);

    HealthCheck getHealthCheck();
}
