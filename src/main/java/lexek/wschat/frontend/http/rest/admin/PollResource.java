package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.GlobalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.PollForm;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.RoomService;
import lexek.wschat.services.poll.PollService;
import lexek.wschat.services.poll.PollState;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/rooms/{roomId}/polls")
@RequiredRole(GlobalRole.ADMIN)
public class PollResource {
    private final RoomService roomService;
    private final PollService pollService;

    public PollResource(RoomService roomService, PollService pollService) {
        this.roomService = roomService;
        this.pollService = pollService;
    }

    @Path("/current")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PollState getCurrentPollForRoom(@PathParam("roomId") @Min(0) long roomId) {
        Room room = roomService.getRoomInstance(roomId);
        return pollService.getActivePoll(room);
    }

    @Path("/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<PollState> getOldPolls(
        @PathParam("roomId") @Min(0) long roomId,
        @QueryParam("page") @Min(0) int page
    ) {
        Room room = roomService.getRoomInstance(roomId);
        return pollService.getOldPolls(room, page);
    }

    @Path("/current")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public PollState createPoll(
        @PathParam("roomId") @Min(0) long roomId,
        @Valid PollForm form,
        @Auth UserDto admin
    ) {
        Room room = roomService.getRoomInstance(roomId);
        return pollService.createPoll(room, admin, form.getQuestion(), form.getOptions());
    }

    @Path("/current")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public PollState closeCurrentPoll(
        @PathParam("roomId") @Min(0) long roomId,
        @Auth UserDto admin
    ) {
        Room room = roomService.getRoomInstance(roomId);
        PollState closedPoll = pollService.getActivePoll(room);
        pollService.closePoll(room, admin);
        return closedPoll;
    }
}
