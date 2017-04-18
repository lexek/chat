package lexek.wschat.db.model.rest;

import lexek.wschat.chat.model.User;
import lexek.wschat.db.jooq.tables.pojos.Ticket;

public class TicketRestModel {
    private final User user;
    private final Ticket ticket;
    private final String closedBy;

    public TicketRestModel(User user, Ticket ticket, String closedBy) {
        this.user = user;
        this.ticket = ticket;
        this.closedBy = closedBy;
    }

    public User getUser() {
        return user;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public String getClosedBy() {
        return closedBy;
    }
}
