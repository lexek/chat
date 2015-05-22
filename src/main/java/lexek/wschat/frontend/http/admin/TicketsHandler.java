package lexek.wschat.frontend.http.admin;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.Ticket;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.TicketService;

public class TicketsHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final TicketService ticketService;

    public TicketsHandler(AuthenticationManager authenticationManager, TicketService ticketService) {
        this.authenticationManager = authenticationManager;
        this.ticketService = ticketService;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        UserAuthDto auth = authenticationManager.checkFullAuthentication(request);
        if (auth != null && auth.getUser() != null && auth.getUser().hasRole(GlobalRole.ADMIN)) {
            if (request.method() == HttpMethod.GET) {
                handleGet(request, response);
                return;
            } else if (request.method() == HttpMethod.POST) {
                handlePost(request, response, auth.getUser());
                return;
            }
        }
        response.badRequest();
    }

    private void handlePost(Request request, Response response, UserDto user) {
        String idParam = request.postParam("id");
        String commentParam = request.postParam("comment");
        if (idParam != null && commentParam != null) {
            Long id = Longs.tryParse(idParam);
            if (id != null) {
                Ticket ticket = ticketService.getTicket(id);
                if (ticket != null) {
                    ticketService.closeTicket(ticket, user, commentParam);
                    response.stringContent("ok");
                    return;
                }
            }
        }
        response.badRequest();
    }

    private void handleGet(Request request, Response response) {
        String pageParam = request.queryParam("page");
        Integer page = pageParam != null ? Ints.tryParse(pageParam) : null;

        String openParam = request.queryParam("open");
        Boolean isOpen = openParam == null || Boolean.parseBoolean(openParam);

        if (page != null && page >= 0) {
            response.jsonContent(ticketService.getAllTickets(isOpen, page));
        } else {
            response.badRequest();
        }
    }
}
