package lexek.wschat.services;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractService implements Service {
    private final ImmutableList<String> availableActions;
    private final String name;
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected AbstractService(String name, ImmutableList<String> availableActions) {
        this.name = name;
        this.availableActions = availableActions;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getAvailableActions() {
        return availableActions;
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
