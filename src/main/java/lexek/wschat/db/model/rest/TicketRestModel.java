package lexek.wschat.db.model.rest;

import lexek.wschat.db.jooq.tables.pojos.Ticket;
import lexek.wschat.db.model.UserDto;

public class TicketRestModel {
    private final UserDto user;
    private final Ticket ticket;
    private final String closedBy;

    public TicketRestModel(UserDto user, Ticket ticket, String closedBy) {
        this.user = user;
        this.ticket = ticket;
        this.closedBy = closedBy;
    }

    public UserDto getUser() {
        return user;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public String getClosedBy() {
        return closedBy;
    }
}
