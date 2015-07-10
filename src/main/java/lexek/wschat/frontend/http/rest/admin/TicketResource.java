package lexek.wschat.frontend.http.rest.admin;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.Ticket;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.CloseTicketForm;
import lexek.wschat.db.model.rest.ErrorModel;
import lexek.wschat.db.model.rest.TicketRestModel;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.services.TicketService;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/tickets")
@RequiredRole(GlobalRole.SUPERADMIN)
public class TicketResource {
    private final TicketService ticketService;

    public TicketResource(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Path("/open/count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map getCount() {
        return ImmutableMap.of("count", ticketService.countOpenTickets());
    }

    @Path("/{state: (open|closed)}/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataPage<TicketRestModel> getAllTickets(
        @PathParam("state") @NotNull String state,
        @QueryParam("page") @Min(0) int page
    ) {
        return ticketService.getAllTickets(state.equals("open"), page);
    }

    @Path("/ticket/{ticketId}/close")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Ticket closeTicket(
        @PathParam("ticketId") @NotNull long ticketId,
        @Auth UserDto admin,
        @Valid CloseTicketForm form
    ) {
        Ticket ticket = ticketService.getTicket(ticketId);
        if (ticket == null) {
            throw new WebApplicationException(Response.status(404).entity(new ErrorModel("Unknown ticket.")).build());
        }
        ticketService.closeTicket(ticket, admin, form.getComment());
        return ticket;
    }
}
