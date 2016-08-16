package lexek.wschat.frontend.http.rest.admin;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.Announcement;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.AnnouncementForm;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.AnnouncementService;
import lexek.wschat.services.RoomService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Map;

@Path("/rooms/{roomId}/announcements")
@RequiredRole(GlobalRole.ADMIN)
public class AnnouncementResource {
    private final AnnouncementService announcementService;
    private final RoomService roomService;

    @Inject
    public AnnouncementResource(AnnouncementService announcementService, RoomService roomService) {
        this.announcementService = announcementService;
        this.roomService = roomService;
    }

    @Path("/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Announcement> getAnnouncementsForRoom(
        @PathParam("roomId") @Min(0) long roomId
    ) {
        Room room = roomService.getRoomInstance(roomId);
        return announcementService.getAnnouncements(room);
    }

    @Path("/new")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Announcement add(
        @PathParam("roomId") @Min(0) long roomId,
        @Auth UserDto user,
        @Valid AnnouncementForm formData
    ) {
        Room room = roomService.getRoomInstance(roomId);
        Announcement announcement;
        if (formData.isOnlyBroadcast()) {
            announcement = announcementService.announceWithoutSaving(formData.getText(), room);
        } else {
            announcement = announcementService.announce(formData.getText(), room, user);
        }
        return announcement;
    }

    @Path("/{announcementId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Map remove(
        @PathParam("announcementId") long announcementId,
        @Auth UserDto user
    ) {
        announcementService.setInactive(announcementId, user);
        return ImmutableMap.of("success", true);
    }
}
