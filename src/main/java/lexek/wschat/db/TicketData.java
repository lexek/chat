package lexek.wschat.db;

import lexek.wschat.db.jooq.tables.pojos.Ticket;

public class TicketData {
    private final UserDto user;
    private final Ticket ticket;
    private final String closedBy;

    public TicketData(UserDto user, Ticket ticket, String closedBy) {
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
