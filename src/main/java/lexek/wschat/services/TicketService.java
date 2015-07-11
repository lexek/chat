package lexek.wschat.services;

import lexek.wschat.chat.*;
import lexek.wschat.db.dao.TicketDao;
import lexek.wschat.db.jooq.tables.pojos.Ticket;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.Email;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.rest.TicketRestModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.List;

public class TicketService {
    private final Logger logger = LoggerFactory.getLogger(TicketService.class);
    private final UserService userService;
    private final EmailService emailService;
    private final TicketDao dao;
    private final MessageBroadcaster messageBroadcaster;

    private int pageLength = 20;

    public TicketService(UserService userService,
                         EmailService emailService,
                         TicketDao dao,
                         MessageBroadcaster messageBroadcaster) {
        this.userService = userService;
        this.emailService = emailService;
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
            UserDto user = userService.fetchById(ticket.getUser());
            if (user != null && user.getEmail() != null && user.hasRole(GlobalRole.USER)) {
                try {
                    emailService.sendEmail(new Email(
                        new InternetAddress(user.getEmail()),
                        "Your ticket is closed",
                        "Your ticket " + ticket.getText() + " was closed by " + closedBy.getName() +
                            " with comment: " + comment));
                } catch (AddressException e) {
                    logger.warn("error while sending notification email", e);
                }
            }
        }
    }

    public DataPage<TicketRestModel> getAllTickets(boolean open, int page) {
        return dao.getAll(open, page, pageLength);
    }

    public DataPage<Ticket> getAllTicketsForUser(UserDto user, int page) {
        return dao.getAll(user, page, 10);
    }

    public List<Ticket> getUnreadTickets(UserDto user) {
        return dao.getNotDeliveredTicketsForUser(user.getId());
    }

    public int countOpenTickets() {
        return dao.getCount();
    }
}
