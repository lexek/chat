package lexek.wschat.frontend.http.admin;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.HistoryService;

import java.util.List;

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
                String roomIdParam = request.queryParam("room");
                if (roomIdParam != null) {
                    Long roomId = Longs.tryParse(roomIdParam);
                    if (roomId != null) {
                        handleGet(request, response, roomId);
                        return;
                    }
                }
            }
        }
        response.badRequest();
    }

    private void handleGet(Request request, Response response, long roomId) {
        String pageParam = request.queryParam("page");
        List<String> users = request.queryParams("user");
        Integer page = pageParam != null ? Ints.tryParse(pageParam) : null;

        if (page != null && page >= 0) {
            if (users != null) {
                if (users.size() < 20) {
                    response.jsonContent(historyService.getAllPagedAsJson(roomId, page, PAGE_LENGTH, users));
                } else {
                    response.badRequest();
                }
            } else {
                response.jsonContent(historyService.getAllPagedAsJson(roomId, page, PAGE_LENGTH));
            }
        } else {
            response.badRequest();
        }
    }

}
