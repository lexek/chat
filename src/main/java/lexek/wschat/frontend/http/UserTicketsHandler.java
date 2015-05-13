package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableSet;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.TicketService;

public class UserTicketsHandler extends SimpleHttpHandler {
    private final ImmutableSet<String> categories = ImmutableSet.of("BAN", "RENAME", "BUG", "OTHER");
    private final AuthenticationManager authenticationManager;
    private final TicketService ticketService;

    public UserTicketsHandler(AuthenticationManager authenticationManager, TicketService ticketService) {
        this.authenticationManager = authenticationManager;
        this.ticketService = ticketService;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        UserAuthDto auth = authenticationManager.checkAuthentication(request);
        if (auth != null && auth.getUser() != null) {
            UserDto user = auth.getUser();
            if (request.method() == HttpMethod.GET) {
                handleGet(request, response, user);
                return;
            } else if (request.method() == HttpMethod.POST) {
                handlePost(request, response, user);
                return;
            }
        }
        response.badRequest();
    }

    private void handlePost(Request request, Response response, UserDto user) {
        String categoryParam = request.postParam("category");
        String textParam = request.postParam("text");
        if (categoryParam != null && textParam != null && categories.contains(categoryParam) && textParam.length() > 5
                && textParam.length() < 1024) {
            if (ticketService.submit(categoryParam, textParam, user)) {
                response.stringContent("ok");
            } else {
                response.badRequest("You cannot have more than 5 tickets at one time.");
            }
        } else {
            response.badRequest();
        }
    }

    private void handleGet(Request request, Response response, UserDto user) {
        response.jsonContent(ticketService.getAllTicketsForUser(user, 0).getData());
    }
}
