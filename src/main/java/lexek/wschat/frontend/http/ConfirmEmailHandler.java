package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.db.UserAuthDto;
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
            boolean success = this.authenticationManager.confirmEmail(code);
            response.renderTemplate("confirm_email", ImmutableMap.of("success", success));
            if (success) {
                final UserAuthDto auth = authenticationManager.checkAuthentication(request);
                if (auth != null && auth.getUser() != null) {
                    connectionManager.forEach(connection -> {
                        Long id = connection.getUser().getId();
                        if (id != null && id.equals(auth.getUser().getId())) {
                            connection.close();
                        }
                    });

                }
            }
        }
    }
}
