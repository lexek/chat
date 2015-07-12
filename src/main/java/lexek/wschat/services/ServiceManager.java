package lexek.wschat.services;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;

import java.util.ArrayList;
import java.util.List;

public class ServiceManager {
    private final HealthCheckRegistry healthCheckRegistry;
    private final MetricRegistry metricRegistry;
    private final List<Service> services = new ArrayList<>();

    public ServiceManager(HealthCheckRegistry healthCheckRegistry, MetricRegistry metricRegistry) {
        this.healthCheckRegistry = healthCheckRegistry;
        this.metricRegistry = metricRegistry;
    }

    public void registerService(Service service) {
        this.services.add(service);
    }

    public List<Service> getServices() {
        return services;
    }

    public void startAll() throws Exception {
        for (Service service : services) {
            service.registerMetrics(metricRegistry);
            healthCheckRegistry.register(service.getName(), service.getHealthCheck());
            service.start();
        }
    }

    public void stopAll() {
        services.forEach(lexek.wschat.services.Service::stop);
    }
}
