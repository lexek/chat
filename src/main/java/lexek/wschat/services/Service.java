package lexek.wschat.services;

import java.util.List;

public interface Service<T> {
    String getName();

    ServiceState getState();

    T getStateData();

    List<String> getAvailableActions();

    void performAction(String action);

    void start() throws Exception;

    void stop();
}
