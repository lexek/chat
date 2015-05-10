package lexek.wschat.frontend.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.LocalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;

public class ChattersApiHandler extends SimpleHttpHandler {
    private final RoomManager roomManager;

    public ChattersApiHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        String roomParam = request.queryParam("room");
        Room room = roomParam != null ? roomManager.getRoomInstance(roomParam) : null;
        if (room != null) {
            JsonArray connections = new JsonArray();
            room.getChatters().stream().filter(chatter -> chatter.hasRole(LocalRole.USER)).forEach(chatter -> {
                JsonObject object = new JsonObject();
                object.addProperty("name", chatter.getUser().getName());
                object.addProperty("timedOut", chatter.getTimeout() != null);
                object.addProperty("banned", chatter.isBanned());
                object.addProperty("role", chatter.getRole().toString());
                object.addProperty("globalRole", chatter.getUser().getRole().toString());
                connections.add(object);
            });
            response.jsonContent(connections);
        } else {
            response.notFound();
        }
    }
}
