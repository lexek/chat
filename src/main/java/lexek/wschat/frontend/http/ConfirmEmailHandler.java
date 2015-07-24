package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;

public class ConfirmEmailHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final ConnectionManager connectionManager;

    public ConfirmEmailHandler(AuthenticationManager authenticationManager, ConnectionManager connectionManager) {
        this.authenticationManager = authenticationManager;
        this.connectionManager = connectionManager;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        String code = request.queryParam("code");
        if (code != null) {
            final UserDto auth = authenticationManager.checkAuthentication(request);
            if (auth != null) {
                boolean success = this.authenticationManager.verifyEmail(code);
                response.renderTemplate("confirm_email", ImmutableMap.of("success", success));
                if (success) {
                    connectionManager.forEach(connection -> {
                        Long id = connection.getUser().getId();
                        if (id != null && id.equals(auth.getId())) {
                            connection.close();
                        }
                    });
                }
            }
        }
    }
}
