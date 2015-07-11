package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.HistoryData;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.HistoryService;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;

@Path("/rooms/{roomId}/history")
@RequiredRole(GlobalRole.ADMIN)
public class HistoryResource {
    private static final int PAGE_LENGTH = 15;

    private final HistoryService historyService;

    public HistoryResource(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Path("/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<HistoryData> getRoomHistory(
        @PathParam("roomId") @Min(0) long roomId,
        @QueryParam("page") @Min(0) int page,
        @QueryParam("user") @Size(max = 20) List<String> users,
        @QueryParam("since") @Min(0) Long since,
        @QueryParam("until") @Min(0) Long until
    ) {
        return historyService.getAllPagedAsJson(roomId,
            page, PAGE_LENGTH,
            users != null && users.isEmpty() ? Optional.empty() : Optional.ofNullable(users),
            Optional.ofNullable(since),
            Optional.ofNullable(until));
    }

    @Path("/peek")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<HistoryData> peekRoomHistory(@PathParam("roomId") @Min(0) long roomId) {
        return historyService.getLast20(roomId);
    }
}
