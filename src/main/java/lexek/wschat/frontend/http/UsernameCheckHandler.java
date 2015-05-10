package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.services.UserService;

public class UsernameCheckHandler extends SimpleHttpHandler {
    private final UserService userService;
    private final Gson gson = new Gson();

    public UsernameCheckHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        String username = request.postParam("username");
        if (username != null) {
            username = username.toLowerCase();
            response.stringContent(gson.toJson(ImmutableMap.of("available", userService.checkIfAvailable(username))), "application/json; charset=utf-8");
        } else {
            response.badRequest();
        }
    }
}
