package lexek.wschat.services.managed;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class ServiceManager {
    private final Logger logger = LoggerFactory.getLogger(ServiceManager.class);
    private final HealthCheckRegistry healthCheckRegistry;
    private final MetricRegistry metricRegistry;
    private final List<ManagedService> services = new ArrayList<>();

    @Inject
    public ServiceManager(
        HealthCheckRegistry healthCheckRegistry,
        @Named("runtimeRegistry") MetricRegistry metricRegistry
    ) {
        this.healthCheckRegistry = healthCheckRegistry;
        this.metricRegistry = metricRegistry;
    }

    @Inject
    public void init(IterableProvider<ManagedService> services) {
        StreamSupport
            .stream(services.spliterator(), false)
            .sorted((o1, o2) -> o1.getStage().ordinal() - o2.getStage().ordinal())
            .forEach(this::registerService);
    }

    public void registerService(ManagedService service) {
        if (service == null) {
            throw new IllegalArgumentException();
        }
        this.services.add(service);
    }

    public List<ManagedService> getServices() {
        return services;
    }

    public void startAll() throws Exception {
        for (ManagedService service : services) {
            service.registerMetrics(metricRegistry);
            logger.info("starting service {} {}", service.getName(), service.getClass().getCanonicalName());
            healthCheckRegistry.register(service.getName(), service.getHealthCheck());
            try {
                service.start();
                logger.info("Service {} started successfully", service.getName());
            } catch (Exception e) {
                logger.error("Error while starting service {}", service.getName(), e);
            }
        }
    }

    public void stopAll() {
        services.forEach(ManagedService::stop);
    }
}
