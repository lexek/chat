package lexek.wschat.frontend.http.rest.admin;

import com.google.common.collect.ImmutableMap;
import lexek.httpserver.*;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.Announcement;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.AnnouncementForm;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.AnnouncementService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/announcements")
@RequiredRole(GlobalRole.ADMIN)
public class AnnouncementResource {
    private final AnnouncementService announcementService;

    public AnnouncementResource(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    //TODO: get announcements for room

    @Path("/room/{roomId}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Map add(@PathParam("roomId") long roomId,
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
        return ImmutableMap.of("success", true);
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
