package lexek.wschat.frontend.http.admin;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;

public class AdminPageHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;

    public AdminPageHandler(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (request.method() == HttpMethod.GET) {
            UserDto user = authenticationManager.checkAuthentication(request);
            if (user != null && user.hasRole(GlobalRole.ADMIN)) {
                response.renderTemplate("admin", ImmutableMap.of("user", user));
            } else {
                response.forbidden();
            }
        } else {
            response.badRequest();
        }
    }
}
