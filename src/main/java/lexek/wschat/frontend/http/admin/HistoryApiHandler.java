package lexek.wschat.frontend.http.admin;

import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.HistoryService;

import java.util.List;
import java.util.Optional;

public class HistoryApiHandler extends SimpleHttpHandler {
    private static final int PAGE_LENGTH = 15;

    private final AuthenticationManager authenticationManager;
    private final HistoryService historyService;

    public HistoryApiHandler(AuthenticationManager authenticationManager, HistoryService historyService) {
        this.authenticationManager = authenticationManager;
        this.historyService = historyService;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (authenticationManager.hasRole(request, GlobalRole.ADMIN)) {
            if (request.method() == HttpMethod.GET) {
                Long roomId = request.queryParamAsLong("room");
                if (roomId != null) {
                    handleGet(request, response, roomId);
                    return;
                }
            }
        }
        response.badRequest();
    }

    private void handleGet(Request request, Response response, long roomId) {
        List<String> users = request.queryParams("user");
        Integer page = request.queryParamAsInteger("page");

        if (page != null && page >= 0) {
            if (users != null) {
                if (users.size() >= 20) {
                    response.badRequest();
                }
            }
            response.jsonContent(historyService.getAllPagedAsJson(roomId, page, PAGE_LENGTH,
                    Optional.ofNullable(users),
                    Optional.ofNullable(request.queryParamAsLong("since")),
                    Optional.ofNullable(request.queryParamAsLong("until"))));
        } else {
            response.badRequest();
        }
    }

}
