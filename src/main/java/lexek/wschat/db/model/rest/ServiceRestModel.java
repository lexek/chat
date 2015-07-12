package lexek.wschat.db.model.rest;

import java.util.List;

public class ServiceRestModel {
    private final String name;
    private final List<String> availableActions;

    public ServiceRestModel(String name, List<String> availableActions) {
        this.name = name;
        this.availableActions = availableActions;
    }

    public String getName() {
        return name;
    }

    public List<String> getAvailableActions() {
        return availableActions;
    }
}
