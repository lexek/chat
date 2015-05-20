package lexek.wschat.frontend.http.admin;

import com.google.common.primitives.Ints;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.UserService;

public class UsersHandler extends SimpleHttpHandler {
    private static final int PAGE_LENGTH = 15;

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public UsersHandler(AuthenticationManager authenticationManager, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
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
        String search = request.queryParam("search");
        Integer page = pageParam != null ? Ints.tryParse(pageParam) : null;

        if (page != null && page >= 0) {
            if (search != null) {
                search = search.replace("!", "!!");
                search = search.replace("%", "!%");
                search = search.replace("_", "!_");
                search = '%' + search + '%';
                response.jsonContent(userService.searchPaged(page, PAGE_LENGTH, search));
            } else {
                response.jsonContent(userService.getAllPaged(page, PAGE_LENGTH));
            }
        } else {
            response.badRequest();
        }
    }
}
