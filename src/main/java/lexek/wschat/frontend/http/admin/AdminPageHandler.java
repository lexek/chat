package lexek.wschat.frontend.http.admin;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.UserAuthDto;
import lexek.wschat.security.AuthenticationManager;

public class AdminPageHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;

    public AdminPageHandler(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (request.method() == HttpMethod.GET) {
            UserAuthDto userAuth = authenticationManager.checkAuthentication(request);
            if (userAuth != null && userAuth.getUser() != null && userAuth.getUser().hasRole(GlobalRole.ADMIN)) {
                response.renderTemplate("admin", ImmutableMap.of("user", userAuth.getUser()));
            } else {
                response.forbidden();
            }
        } else {
            response.badRequest();
        }
    }
}
