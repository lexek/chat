package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.dao.ChatterDao;
import lexek.wschat.db.model.ChatterData;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.jersey.RequiredRole;

import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/chatters")
@RequiredRole(GlobalRole.ADMIN)
public class ChattersResource {
    private static final int PAGE_LENGTH = 10;
    private final ChatterDao chatterDao;

    public ChattersResource(ChatterDao chatterDao) {
        this.chatterDao = chatterDao;
    }

    @GET
    @Path("/{roomId}")
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<ChatterData> getChatters(
        @Min(0) @PathParam("roomId") long roomId,
        @Min(0) @QueryParam("page") int page,
        @QueryParam("search") String search
    ) {
        if (search != null) {
            search = search.replace("!", "!!");
            search = search.replace("%", "!%");
            search = search.replace("_", "!_");
            search = '%' + search + '%';
            return chatterDao.searchPaged(roomId, page, PAGE_LENGTH, search);
        } else {
            return chatterDao.getAllPaged(roomId, page, PAGE_LENGTH);
        }
    }
}
