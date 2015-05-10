package lexek.wschat.frontend.http.admin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.security.AuthenticationManager;

public class OnlineHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final ConnectionManager connectionManager;
    private final Gson gson = new Gson();

    public OnlineHandler(AuthenticationManager authenticationManager, ConnectionManager connectionManager) {
        this.authenticationManager = authenticationManager;
        this.connectionManager = connectionManager;
    }


    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (request.method() == HttpMethod.GET) {
            if (authenticationManager.hasRole(request, GlobalRole.SUPERADMIN)) {
                JsonArray connections = new JsonArray();
                for (Connection c : connectionManager.getConnections()) {
                    JsonObject object = new JsonObject();
                    object.addProperty("ip", c.getIp());
                    object.add("user", gson.toJsonTree(c.getUser().getWrappedObject()));
                    connections.add(object);
                }
                response.stringContent(gson.toJson(connections), "application/json; charset=utf-8");
                return;
            }
        }
        response.badRequest();
    }
}
