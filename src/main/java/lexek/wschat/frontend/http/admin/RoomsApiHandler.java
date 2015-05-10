package lexek.wschat.frontend.http.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.db.UserAuthDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.util.Names;

public class RoomsApiHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final RoomManager roomManager;

    public RoomsApiHandler(AuthenticationManager authenticationManager, RoomManager roomManager) {
        this.authenticationManager = authenticationManager;
        this.roomManager = roomManager;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        UserAuthDto authDto = authenticationManager.checkAuthentication(request);
        if (authDto != null && authDto.getUser() != null) {
            if (request.method() == HttpMethod.GET) {
                if (authDto.getUser().hasRole(GlobalRole.ADMIN)) {
                    response.jsonContent(generateJson());
                    return;
                }
            } else if (request.method() == HttpMethod.POST) {
                if (authDto.getUser().hasRole(GlobalRole.SUPERADMIN)) {
                    String action = request.postParam("action");
                    String name = request.postParam("name");
                    if (action != null && name != null) {
                        if (action.equals("ADD") && Names.ROOM_PATTERN.matcher(name).matches()) {
                            String topic = request.postParam("topic");
                            roomManager.createRoom(new lexek.wschat.db.jooq.tables.pojos.Room(null, name, topic));
                            response.stringContent("ok");
                            return;
                        } else if (action.equals("DELETE")) {
                            roomManager.deleteRoom(name);
                            response.stringContent("ok");
                            return;
                        }
                    }
                }
            }
        }
        response.badRequest();
    }

    private JsonArray generateJson() {
        JsonArray array = new JsonArray();
        for (Room room : roomManager.getRooms()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", room.getId());
            object.addProperty("name", room.getName());
            object.addProperty("topic", room.getTopic());
            object.addProperty("online", room.getOnline());
            array.add(object);
        }
        return array;
    }

}
