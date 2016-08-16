package lexek.wschat.services.managed;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractManagedService implements ManagedService {
    private final String name;
    private final InitStage initStage;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected AbstractManagedService(String name, InitStage initStage) {
        this.name = name;
        this.initStage = initStage;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InitStage getStage() {
        return initStage;
    }

    @Override
    public abstract void start();

    @Override
    public void registerMetrics(MetricRegistry metricRegistry) {
        //do nothing
    }
}
