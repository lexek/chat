package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.model.OnlineUser;
import lexek.wschat.security.jersey.RequiredRole;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/users")
@RequiredRole(GlobalRole.ADMIN)
public class UsersResource {
    private final ConnectionManager connectionManager;

    public UsersResource(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Path("/online")
    @RequiredRole(GlobalRole.SUPERADMIN)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<OnlineUser> getOnlineUsers() {
        return connectionManager.getConnections()
            .stream()
            .map(connection -> new OnlineUser(connection.getIp(), connection.getUser().getWrappedObject()))
            .collect(Collectors.toList());
    }
}
