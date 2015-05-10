package lexek.wschat.services;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractService<T> implements Service<T> {
    private final ImmutableList<String> availableActions;
    private final String name;
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected ServiceState state = ServiceState.NEW;
    protected T stateData = null;

    protected AbstractService(String name, ImmutableList<String> availableActions) {
        this.name = name;
        this.availableActions = availableActions;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ServiceState getState() {
        return state;
    }

    @Override
    public T getStateData() {
        return stateData;
    }

    @Override
    public List<String> getAvailableActions() {
        return availableActions;
    }

    @Override
    public void start() {
        logger.info("Starting service {}", name);
        this.state = ServiceState.STARTING;
        try {
            start0();
            logger.info("Service {} started successfully", name);
            this.state = ServiceState.RUNNING;
        } catch (Exception e) {
            logger.error("Error while starting service {}", name, e);
        }
    }

    protected abstract void start0();
}
