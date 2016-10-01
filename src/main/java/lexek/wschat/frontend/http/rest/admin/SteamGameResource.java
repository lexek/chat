package lexek.wschat.frontend.http.rest.admin;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.SteamGameService;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/steamGames")
public class SteamGameResource {
    private final SteamGameService steamGameService;

    @Inject
    public SteamGameResource(SteamGameService steamGameService) {
        this.steamGameService = steamGameService;
    }

    @Path("/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredRole(GlobalRole.UNAUTHENTICATED)
    public Map<String, String> getName(
        @PathParam("id") @Min(0) long id
    ) {
        String name = steamGameService.getName(id);
        if (name == null) {
            throw new EntityNotFoundException();
        }
        return ImmutableMap.of(
            "name", name
        );
    }

    @Path("/syncDb")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredRole(GlobalRole.SUPERADMIN)
    public void syncDb() {
        steamGameService.syncDatabase();
    }
}
