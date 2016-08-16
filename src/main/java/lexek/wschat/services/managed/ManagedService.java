package lexek.wschat.services.managed;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface ManagedService {
    String getName();

    void start() throws Exception;

    void stop();

    void registerMetrics(MetricRegistry metricRegistry);

    HealthCheck getHealthCheck();

    InitStage getStage();
}
