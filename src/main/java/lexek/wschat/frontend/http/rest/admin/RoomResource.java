package lexek.wschat.frontend.http.rest.admin;

import com.fasterxml.jackson.databind.JsonNode;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.model.form.RoomForm;
import lexek.wschat.db.model.rest.ErrorModel;
import lexek.wschat.db.model.rest.RoomRestModel;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.RoomService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/rooms")
@RequiredRole(GlobalRole.ADMIN)
public class RoomResource {
    private final RoomService roomService;

    @Inject
    public RoomResource(RoomService roomService) {
        this.roomService = roomService;
    }

    @Path("/{roomId}/info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RoomRestModel getRoom(@PathParam("roomId") @Min(0) long roomId) {
        Room room = roomService.getRoomInstance(roomId);
        if (room == null) {
            throw new WebApplicationException(Response.status(400).entity(new ErrorModel("Unknown room.")).build());
        }
        return new RoomRestModel(room.getId(), room.getName(), room.getTopic(), room.getOnlineCount());
    }

    @Path("/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<RoomRestModel> getAllRooms() {
        return roomService.getRooms()
            .stream()
            .map(room -> new RoomRestModel(
                room.getId(),
                room.getName(),
                room.getTopic(),
                room.getOnlineCount()
            ))
            .collect(Collectors.toList());
    }

    @Path("/new")
    @RequiredRole(GlobalRole.SUPERADMIN)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void createRoom(
        @Valid @NotNull RoomForm roomForm,
        @Auth User admin
    ) {
        roomService.createRoom(roomForm.getName(), roomForm.getTopic(), admin);
    }

    @Path("/{roomId}")
    @RequiredRole(GlobalRole.SUPERADMIN)
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void deleteRoom(
        @PathParam("roomId") @Min(0) long roomId,
        @Auth User admin
    ) {
        Room room = roomService.getRoomInstance(roomId);
        roomService.deleteRoom(room, admin);
    }

    @Path("/{roomId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTopic(
        @PathParam("roomId") @Min(0) long roomId,
        @Auth User admin,
        JsonNode data
    ) {
        Room room = roomService.getRoomInstance(roomId);
        JsonNode topicNode = data.get("topic");
        if (topicNode.isNull()) {
            throw new InvalidInputException("topic", "Shouldn't be null");
        }
        if (!topicNode.isTextual()) {
            throw new InvalidInputException("topic", "Should be string type");
        }
        String topic = topicNode.asText().trim();
        if (topic.length() > 8096) {
            throw new InvalidInputException("topic", "Too long");
        }
        if (topic.length() == 0) {
            throw new InvalidInputException("topic", "Shouldn't be empty or only spaces");
        }
        roomService.updateTopic(admin, room, topic);
        return Response.ok().build();
    }
}
