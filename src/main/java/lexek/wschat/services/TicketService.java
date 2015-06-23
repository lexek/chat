package lexek.wschat.services;

import lexek.wschat.chat.*;
import lexek.wschat.db.dao.TicketDao;
import lexek.wschat.db.jooq.tables.pojos.Ticket;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.TicketData;
import lexek.wschat.db.model.UserDto;

public class TicketService {
    private final TicketDao dao;
    private final MessageBroadcaster messageBroadcaster;

    private int pageLength = 20;

    public TicketService(TicketDao dao, MessageBroadcaster messageBroadcaster) {
        this.dao = dao;
        this.messageBroadcaster = messageBroadcaster;
    }

    public void setPageLength(int pageLength) {
        this.pageLength = pageLength;
    }

    public Ticket getTicket(long id) {
        return dao.getById(id);
    }

    public boolean submit(String category, String text, UserDto user) {
        Ticket ticket = new Ticket(null, System.currentTimeMillis(), user.getId(), true, category, text, null, null, null);
        boolean result = dao.add(ticket);
        if (result) {
            String message = "New ticket opened by " + user.getName() + ": " + ticket.getText();
            messageBroadcaster.submitMessage(Message.infoMessage(message), Connection.STUB_CONNECTION, GlobalRole.SUPERADMIN.FILTER);
        }
        return result;
    }

    public void closeTicket(Ticket ticket, UserDto closedBy, String comment) {
        if (ticket.getIsOpen()) {
            ticket.setIsOpen(false);
            ticket.setClosedBy(closedBy.getId());
            ticket.setAdminReply(comment);
            dao.update(ticket);
            Message msg = Message.infoMessage("Your ticket " + ticket.getText() + " was closed by " + closedBy.getName() +
                " with comment: " + comment);
            messageBroadcaster.submitMessage(msg, Connection.STUB_CONNECTION, new UserIdFilter(ticket.getId()));
        }
    }

    public DataPage<TicketData> getAllTickets(boolean open, int page) {
        return dao.getAll(open, page, pageLength);
    }

    public DataPage<Ticket> getAllTicketsForUser(UserDto user, int page) {
        return dao.getAll(user, page, 10);
    }

    public int countOpenTickets() {
        return dao.getCount();
    }
}
