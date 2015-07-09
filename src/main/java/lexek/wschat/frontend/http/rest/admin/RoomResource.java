package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.GlobalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.RoomForm;
import lexek.wschat.db.model.rest.RoomRestModel;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/rooms")
@RequiredRole(GlobalRole.ADMIN)
public class RoomResource {
    private final RoomManager roomManager;

    public RoomResource(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Path("/{roomId}/info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RoomRestModel getRoom(@PathParam("roomId") @Min(0) long roomId) {
        Room room = roomManager.getRoomInstance(roomId);
        if (room == null) {
            throw new WebApplicationException(400);
        }
        return new RoomRestModel(room.getId(), room.getName(), room.getTopic(), room.getOnline());
    }

    @Path("/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<RoomRestModel> getAllRooms() {
        return roomManager.getRooms()
            .stream()
            .map(room -> new RoomRestModel(
                room.getId(),
                room.getName(),
                room.getTopic(),
                room.getOnline()
            ))
            .collect(Collectors.toList());
    }

    @Path("/new")
    @RequiredRole(GlobalRole.SUPERADMIN)
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void createRoom(
        @Valid @NotNull RoomForm roomForm,
        @Auth UserDto admin
    ) {
        roomManager.createRoom(roomForm.getName(), roomForm.getTopic(), admin);
    }

    @Path("/{roomId}")
    @RequiredRole(GlobalRole.SUPERADMIN)
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void deleteRoom(
        @PathParam("roomId") @Min(0) long roomId,
        @Auth UserDto admin
    ) {
        Room room = roomManager.getRoomInstance(roomId);
        if (room == null) {
            throw new WebApplicationException(400);
        }
        roomManager.deleteRoom(room, admin);
    }
}
