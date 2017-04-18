package lexek.wschat.frontend.http.rest;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.jooq.tables.pojos.Ticket;
import lexek.wschat.db.model.rest.BasicResponse;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.services.TicketService;
import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Inject;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/userTicket")
public class UserTicketResource {
    private final ImmutableSet<String> categories = ImmutableSet.of("EMOTICON", "BAN", "RENAME", "BUG", "OTHER");
    private final TicketService ticketService;

    @Inject
    public UserTicketResource(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public BasicResponse handlePost(
        @NotEmpty @FormParam("category") String category,
        @NotEmpty @Size(min = 5, max = 1024) @FormParam("text") String text,
        @Auth User user
    ) {
        if (categories.contains(category) && text.length() > 5 && text.length() < 1024) {
            if (ticketService.submit(category, text, user)) {
                return new BasicResponse(true, null);
            } else {
                return new BasicResponse(false, "You cannot have more than 5 tickets at one time.");
            }
        }
        return new BasicResponse(false, "bad request");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Ticket> getTickets(@Auth User user) {
        return ticketService.getAllTicketsForUser(user, 0).getData();
    }

}
