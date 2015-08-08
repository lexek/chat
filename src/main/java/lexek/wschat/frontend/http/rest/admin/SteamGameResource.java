package lexek.wschat.frontend.http.rest.admin;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.model.e.EntityNotFoundException;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.SteamGameResolver;

import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/steamGames")
public class SteamGameResource {
    private final SteamGameResolver steamGameResolver;

    public SteamGameResource(SteamGameResolver steamGameResolver) {
        this.steamGameResolver = steamGameResolver;
    }

    @Path("/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredRole(GlobalRole.UNAUTHENTICATED)
    public Map<String, String> getName(
        @PathParam("id") @Min(0) long id
    ) {
        String name = steamGameResolver.getName(id);
        if (name == null) {
            throw new EntityNotFoundException();
        }
        return ImmutableMap.of(
            "name", name
        );
    }

    @Path("/syncDb")
    @POST
    @RequiredRole(GlobalRole.SUPERADMIN)
    public void syncDb() {
        steamGameResolver.syncDatabase();
    }
}
