package lexek.wschat.services;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractService implements Service {
    private final String name;
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected AbstractService(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void start() {
        logger.info("Starting service {}", name);
        try {
            start0();
            logger.info("Service {} started successfully", name);
        } catch (Exception e) {
            logger.error("Error while starting service {}", name, e);
        }
    }

    protected abstract void start0();

    @Override
    public void registerMetrics(MetricRegistry metricRegistry) {
        //do nothing
    }
}
