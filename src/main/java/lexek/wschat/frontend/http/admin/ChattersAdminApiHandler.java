package lexek.wschat.frontend.http.admin;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.dao.ChatterDao;
import lexek.wschat.security.AuthenticationManager;

public class ChattersAdminApiHandler extends SimpleHttpHandler {
    private static final int PAGE_LENGTH = 10;

    private final AuthenticationManager authenticationManager;
    private final ChatterDao chatterDao;

    public ChattersAdminApiHandler(AuthenticationManager authenticationManager, ChatterDao chatterDao) {
        this.authenticationManager = authenticationManager;
        this.chatterDao = chatterDao;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (authenticationManager.hasRole(request, GlobalRole.ADMIN)) {
            if (request.method() == HttpMethod.GET) {
                handleGet(request, response);
                return;
            }
        }
        response.badRequest();
    }

    private void handleGet(Request request, Response response) {
        String pageParam = request.queryParam("page");
        String roomParam = request.queryParam("room");
        String search = request.queryParam("search");

        Integer page = pageParam != null ? Ints.tryParse(pageParam) : null;
        Long room = roomParam != null ? Longs.tryParse(roomParam) : null;

        if (page != null && page >= 0 && room != null) {
            if (search != null) {
                search = search.replace("!", "!!");
                search = search.replace("%", "!%");
                search = search.replace("_", "!_");
                search = '%' + search + '%';
                response.jsonContent(chatterDao.searchPaged(room, page, PAGE_LENGTH, search));
            } else {
                response.jsonContent(chatterDao.getAllPaged(room, page, PAGE_LENGTH));
            }
        } else {
            response.badRequest();
        }
    }
}
