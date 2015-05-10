package lexek.wschat.services;

import java.util.ArrayList;
import java.util.List;

public class ServiceManager {
    private final List<Service> services = new ArrayList<>();

    public void registerService(Service service) {
        this.services.add(service);
    }

    public List<Service> getServices() {
        return services;
    }

    public void startAll() throws Exception {
        for (Service service : services) {
            service.start();
        }
    }

    public void stopAll() {
        services.forEach(lexek.wschat.services.Service::stop);
    }
}
