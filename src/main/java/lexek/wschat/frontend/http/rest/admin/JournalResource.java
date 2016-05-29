package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.dao.JournalDao;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.JournalEntry;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.JournalService;

import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.Set;

@Path("/journal")
@RequiredRole(GlobalRole.ADMIN)
public class JournalResource {
    private static final int PAGE_LENGTH = 15;

    private final JournalService journalService;

    public JournalResource(JournalService journalService) {
        this.journalService = journalService;
    }

    @Path("/room/{roomId}/peek")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<JournalEntry> peekRoomJournal(
        @PathParam("roomId") @Min(0) long roomId,
        @QueryParam("category") Set<String> categories
    ) {
        return journalService.getRoomJournal(0, 3, roomId, Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Path("/room/{roomId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<JournalEntry> getRoomJournal(
        @PathParam("roomId") @Min(0) long roomId,
        @QueryParam("page") @Min(0) int page,
        @QueryParam("category") Set<String> categories,
        @QueryParam("admin") @Min(0) Long adminId,
        @QueryParam("user") @Min(0) Long userId
    ) {
        return journalService.getRoomJournal(
            page,
            PAGE_LENGTH,
            roomId,
            categories.isEmpty() ? Optional.empty() : Optional.of(categories),
            Optional.ofNullable(userId),
            Optional.ofNullable(adminId)
        );
    }

    @Path("/global")
    @RequiredRole(GlobalRole.SUPERADMIN)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<JournalEntry> getGlobalJournal(
        @QueryParam("page") @Min(0) int page,
        @QueryParam("category") Set<String> categories,
        @QueryParam("admin") @Min(0) Long adminId,
        @QueryParam("user") @Min(0) Long userId
    ) {
        return journalService.getGlobalJournal(
            page,
            PAGE_LENGTH,
            categories.isEmpty() ? Optional.empty() : Optional.of(categories),
            Optional.ofNullable(userId),
            Optional.ofNullable(adminId)
        );
    }

    @Path("/categories/global")
    @RequiredRole(GlobalRole.SUPERADMIN)
    @GET
    @Produces
    public Set<String> getGlobalCategories() {
        return journalService.getGlobalCategories().keySet();
    }

    @Path("/categories/room")
    @GET
    @Produces
    public Set<String> getRoomCategories() {
        return journalService.getRoomCategories().keySet();
    }

}
