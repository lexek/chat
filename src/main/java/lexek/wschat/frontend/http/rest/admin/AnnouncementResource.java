package lexek.wschat.frontend.http.rest.admin;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.db.jooq.tables.pojos.Announcement;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.AnnouncementForm;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.AnnouncementService;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Path("/rooms/{roomId}/announcements")
@RequiredRole(GlobalRole.ADMIN)
public class AnnouncementResource {
    private final AnnouncementService announcementService;
    private final RoomManager roomManager;

    public AnnouncementResource(AnnouncementService announcementService, RoomManager roomManager) {
        this.announcementService = announcementService;
        this.roomManager = roomManager;
    }

    @Path("/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Announcement> getAnnouncementsForRoom(
        @PathParam("roomId") @Min(0) long roomId
    ) {
        Room room = roomManager.getRoomInstance(roomId);
        if (room == null) {
            throw new WebApplicationException(400);
        }
        return announcementService.getAnnouncements(room);
    }

    @Path("/new")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Announcement add(@PathParam("roomId") @Min(0) long roomId,
                   @Auth UserDto user,
                   @Valid AnnouncementForm formData
    ) {
        String text = formData.getText();
        Announcement announcement = new Announcement(null, roomId, true, text);
        if (formData.isOnlyBroadcast()) {
            announcementService.announceWithoutSaving(announcement);
        } else {
            announcementService.announce(announcement, user);
        }
        return announcement;
    }

    @Path("/{announcementId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Map remove(@PathParam("announcementId") long announcementId,
                      @Auth UserDto user
    ) {
        announcementService.setInactive(announcementId, user);
        return ImmutableMap.of("success", true);
    }
}
