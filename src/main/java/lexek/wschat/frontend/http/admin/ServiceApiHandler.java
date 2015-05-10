package lexek.wschat.frontend.http.admin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.Service;
import lexek.wschat.services.ServiceManager;

public class ServiceApiHandler extends SimpleHttpHandler {
    private final Gson gson = new Gson();
    private final AuthenticationManager authenticationManager;
    private final ServiceManager serviceManager;

    public ServiceApiHandler(AuthenticationManager authenticationManager, ServiceManager serviceManager) {
        this.authenticationManager = authenticationManager;
        this.serviceManager = serviceManager;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (authenticationManager.hasRole(request, GlobalRole.SUPERADMIN)) {
            if (request.method() == HttpMethod.GET) {
                response.stringContent(generateJson(), "application/json; charset=utf-8");
                return;
            }
        }
        response.badRequest();
    }

    private String generateJson() {
        JsonArray array = new JsonArray();
        for (Service service : serviceManager.getServices()) {
            JsonObject object = new JsonObject();
            object.addProperty("name", service.getName());
            object.addProperty("state", service.getState().toString());
            object.add("stateData", gson.toJsonTree(service.getStateData()));
            object.add("availableActions", gson.toJsonTree(service.getAvailableActions()));
            array.add(object);
        }
        return gson.toJson(array);
    }
}
