package lexek.wschat.frontend.http.admin;

import com.google.common.primitives.Longs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.chat.LocalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.db.jooq.tables.pojos.Announcement;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.AnnouncementService;
import lexek.wschat.services.HistoryService;

public class RoomApiHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final RoomManager roomManager;
    private final HistoryService historyService;
    private final AnnouncementService announcementService;

    public RoomApiHandler(AuthenticationManager authenticationManager,
                          RoomManager roomManager,
                          HistoryService historyService,
                          AnnouncementService announcementService) {
        this.authenticationManager = authenticationManager;
        this.roomManager = roomManager;
        this.historyService = historyService;
        this.announcementService = announcementService;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (authenticationManager.hasRole(request, GlobalRole.ADMIN)) {
            if (request.method() == HttpMethod.GET) {
                String idParam = request.queryParam("id");
                Long roomId = idParam != null ? Longs.tryParse(idParam) : null;
                if (roomId != null) {
                    response.jsonContent(generateJson(roomId));
                }
                return;
            }
        }
        response.badRequest();
    }

    private JsonObject generateJson(long roomId) {
        Room room = roomManager.getRoomInstance(roomId);
        JsonObject rootObject = new JsonObject();
        {
            JsonObject roomObject = new JsonObject();
            roomObject.addProperty("id", room.getId());
            roomObject.addProperty("name", room.getName());
            roomObject.addProperty("topic", room.getTopic());
            roomObject.addProperty("online", room.getOnline());
            rootObject.add("room", roomObject);
        }
        {
            JsonArray chatters = new JsonArray();
            room.getChatters().stream().filter(chatter -> chatter.hasRole(LocalRole.USER)).forEach(chatter -> {
                JsonObject object = new JsonObject();
                object.addProperty("id", chatter.getId());
                object.addProperty("userId", chatter.getUser().getId());
                object.addProperty("name", chatter.getUser().getName());
                object.addProperty("timedOut", chatter.getTimeout() != null);
                object.addProperty("banned", chatter.isBanned());
                object.addProperty("role", chatter.getRole().toString());
                chatters.add(object);
            });
            rootObject.add("chatters", chatters);
        }
        rootObject.add("history", historyService.getLast20(roomId));
        {
            JsonArray announcements = new JsonArray();
            for (Announcement announcement : announcementService.getAnnouncements(room)) {
                JsonObject object = new JsonObject();
                object.addProperty("id", announcement.getId());
                object.addProperty("active", announcement.getActive());
                object.addProperty("text", announcement.getText());
                announcements.add(object);
            }
            rootObject.add("announcements", announcements);
        }

        return rootObject;
    }
}
