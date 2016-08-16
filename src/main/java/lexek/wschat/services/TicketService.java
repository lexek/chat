package lexek.wschat.services;

import lexek.wschat.db.dao.TicketDao;
import lexek.wschat.db.jooq.tables.pojos.Ticket;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.rest.TicketRestModel;
import lexek.wschat.db.tx.Transactional;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class TicketService {
    private static final int PAGE_LENGTH = 20;

    private final UserService userService;
    private final NotificationService notificationService;
    private final TicketDao dao;

    @Inject
    public TicketService(UserService userService, NotificationService notificationService, TicketDao dao) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.dao = dao;
    }

    public Ticket getTicket(long id) {
        return dao.getById(id);
    }

    @Transactional
    public boolean submit(String category, String text, UserDto user) {
        Ticket ticket = new Ticket(null, System.currentTimeMillis(), user.getId(), true, category, text, null, null, null);
        boolean result = dao.add(ticket);
        if (result) {
            if (category.equals("BUG")) {
                notificationService.notifyAdmins(
                    "New bug reported",
                    "New bug reported by " + user.getName() + ": " + ticket.getText(),
                    true
                );
            } else {
                notificationService.notifyAdmins(
                    "New ticket",
                    "New ticket opened by " + user.getName() + ": " + ticket.getText(),
                    true
                );
            }
        }
        return result;
    }

    @Transactional
    public void closeTicket(Ticket ticket, UserDto closedBy, String comment) {
        if (ticket.getIsOpen()) {
            ticket.setIsOpen(false);
            ticket.setClosedBy(closedBy.getId());
            ticket.setAdminReply(comment);

            dao.update(ticket);

            String text = "Your ticket " + ticket.getText() + " was closed by " + closedBy.getName() +
                " with comment: " + comment;

            UserDto user = userService.fetchById(ticket.getUser());
            notificationService.notify(user, "Your ticket is closed", text, true);
        }
    }

    public DataPage<TicketRestModel> getAllTickets(boolean open, int page) {
        return dao.getAll(open, page, PAGE_LENGTH);
    }

    public DataPage<Ticket> getAllTicketsForUser(UserDto user, int page) {
        return dao.getAll(user, page, 10);
    }

    public int countOpenTickets() {
        return dao.getCount();
    }
}
