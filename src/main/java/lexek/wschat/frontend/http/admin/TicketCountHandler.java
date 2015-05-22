package lexek.wschat.frontend.http.admin;

import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.TicketService;

public class TicketCountHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final TicketService ticketService;

    public TicketCountHandler(AuthenticationManager authenticationManager, TicketService ticketService) {
        this.authenticationManager = authenticationManager;
        this.ticketService = ticketService;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (authenticationManager.hasRole(request, GlobalRole.ADMIN)) {
            if (request.method() == HttpMethod.GET) {
                response.stringContent(String.valueOf(ticketService.countOpenTickets()));
                return;
            }
        }
        response.badRequest();
    }
}
