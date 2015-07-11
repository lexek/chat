package lexek.wschat.db.model.rest;

import lexek.wschat.services.ServiceState;

import java.util.List;

public class ServiceRestModel {
    private final String name;
    private final ServiceState state;
    private final Object stateData;
    private final List<String> availableActions;

    public ServiceRestModel(String name, ServiceState state, Object stateData, List<String> availableActions) {
        this.name = name;
        this.state = state;
        this.stateData = stateData;
        this.availableActions = availableActions;
    }

    public String getName() {
        return name;
    }

    public ServiceState getState() {
        return state;
    }

    public Object getStateData() {
        return stateData;
    }

    public List<String> getAvailableActions() {
        return availableActions;
    }
}
