package lexek.wschat.frontend.http.rest.admin;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.chat.LocalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.db.dao.ChatterDao;
import lexek.wschat.db.model.ChatterData;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.rest.ChatterRestModel;
import lexek.wschat.db.model.rest.ErrorModel;
import lexek.wschat.security.jersey.RequiredRole;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/rooms/{roomId}/chatters")
@RequiredRole(GlobalRole.ADMIN)
public class ChattersResource {
    private static final int PAGE_LENGTH = 10;
    private final ChatterDao chatterDao;
    private final RoomManager roomManager;

    public ChattersResource(ChatterDao chatterDao, RoomManager roomManager) {
        this.chatterDao = chatterDao;
        this.roomManager = roomManager;
    }

    //TODO: better representation
    @GET
    @RequiredRole(GlobalRole.UNAUTHENTICATED)
    @Path("/publicList")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map> getChatters(
        @PathParam("roomId") @Size(min = 2, max = 10) String roomName
    ) {
        Room room = roomManager.getRoomInstance(roomName);
        if (room == null) {
            throw new WebApplicationException(Response
                .status(404)
                .entity(new ErrorModel("Unknown room."))
                .build());
        }
        return room.getChatters()
            .stream()
            .filter(chatter -> chatter.hasRole(LocalRole.USER))
            .map(chatter -> ImmutableMap.of(
                "name", chatter.getUser().getName(),
                "timedOut", chatter.getTimeout() != null,
                "banned", chatter.isBanned(),
                "role", chatter.getRole(),
                "globalRole", chatter.getUser().getRole()
            ))
            .collect(Collectors.toList());
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<ChatterData> getChatters(
        @PathParam("roomId") @Min(0) long roomId,
        @QueryParam("page") @Min(0) int page,
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

    //TODO: better representation
    @GET
    @Path("/online")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ChatterRestModel> getOnlineChatters(@Min(0) @PathParam("roomId") long roomId) {
        Room room = roomManager.getRoomInstance(roomId);
        if (room == null) {
            throw new WebApplicationException(Response
                .status(404)
                .entity(new ErrorModel("Unknown room."))
                .build());
        }
        return room.getChatters()
            .stream()
            .filter(chatter -> chatter.hasRole(LocalRole.USER))
            .map(chatter -> new ChatterRestModel(
                chatter.getId(),
                chatter.getUser().getId(),
                chatter.getUser().getName(),
                chatter.getTimeout() != null,
                chatter.isBanned(),
                chatter.getRole()
            ))
            .collect(Collectors.toList());
    }
}
