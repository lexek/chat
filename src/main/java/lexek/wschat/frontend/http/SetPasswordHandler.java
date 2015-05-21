package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.util.Names;
import org.mindrot.jbcrypt.BCrypt;

public class SetPasswordHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;

    public SetPasswordHandler(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        //TODO: require old password
        UserAuthDto auth = authenticationManager.checkFullAuthentication(request);
        if (auth != null && auth.getUser() != null) {
            if (request.method() == HttpMethod.POST) {
                String passwordParam = request.postParam("password");
                if (passwordParam != null && Names.PASSWORD_PATTERN.matcher(passwordParam).matches()) {
                    authenticationManager.setPassword(auth.getUser().getId(), BCrypt.hashpw(passwordParam, BCrypt.gensalt()));
                    response.jsonContent(ImmutableMap.of("success", true));
                } else {
                    response.jsonContent(ImmutableMap.of("success", false, "error", "Bad password format."));
                }
            }
        } else {
            response.forbidden();
        }
    }
}
