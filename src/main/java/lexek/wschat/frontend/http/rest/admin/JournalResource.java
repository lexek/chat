package lexek.wschat.frontend.http.rest.admin;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.dao.JournalDao;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.JournalEntry;
import lexek.wschat.security.jersey.RequiredRole;

import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/journal")
@RequiredRole(GlobalRole.ADMIN)
public class JournalResource {
    private static final int PAGE_LENGTH = 15;

    private final JournalDao journalDao;

    public JournalResource(JournalDao journalDao) {
        this.journalDao = journalDao;
    }

    @Path("/room/{roomId}/peek")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<JournalEntry> peekRoomJournal(
        @PathParam("roomId") @Min(0) long roomId
    ) {
        return journalDao.fetchAllForRoom(0, 3, roomId);
    }

    @Path("/room/{roomId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<JournalEntry> getRoomJournal(
        @PathParam("roomId") @Min(0) long roomId,
        @QueryParam("page") @Min(0) int page
    ) {
        return journalDao.fetchAllForRoom(page, PAGE_LENGTH, roomId);
    }

    @Path("/global")
    @RequiredRole(GlobalRole.SUPERADMIN)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<JournalEntry> getGlobalJournal(@QueryParam("page") @Min(0) int page) {
        return journalDao.fetchAllGlobal(page, PAGE_LENGTH);
    }
}
