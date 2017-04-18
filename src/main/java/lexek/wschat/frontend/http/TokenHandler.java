package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.model.User;
import lexek.wschat.security.AuthenticationManager;

public class TokenHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;

    public TokenHandler(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        User user = authenticationManager.checkFullAuthentication(request);
        if (user != null) {
            if (request.method() == HttpMethod.POST) {
                String token = authenticationManager.generateTokenForUser(user);
                if (token != null) {
                    response.jsonContent(ImmutableMap.of("token", token));
                } else {
                    response.internalError();
                }
            }
        } else {
            response.forbidden();
        }
    }

}
