package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.model.rest.ServiceRestModel;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.Service;
import lexek.wschat.services.ServiceManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/services")
@RequiredRole(GlobalRole.SUPERADMIN)
public class ServicesResource {
    private final ServiceManager serviceManager;

    public ServicesResource(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    @Path("/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ServiceRestModel> getServices() {
        return serviceManager.getServices()
            .stream()
            .map(service -> (Service<Object>) service)
            .map(service -> new ServiceRestModel(
                service.getName(),
                service.getAvailableActions()
            ))
            .collect(Collectors.toList());
    }
}
