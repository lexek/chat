package lexek.wschat.frontend.http.admin;

import com.google.common.primitives.Longs;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.PollService;

import java.util.List;

public class PollApiHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final RoomManager roomManager;
    private final PollService pollService;

    public PollApiHandler(AuthenticationManager authenticationManager, RoomManager roomManager, PollService pollService) {
        this.authenticationManager = authenticationManager;
        this.roomManager = roomManager;
        this.pollService = pollService;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (authenticationManager.hasRole(request, GlobalRole.ADMIN)) {
            if (request.method() == HttpMethod.GET) {
                String roomParam = request.queryParam("room");
                Long roomId = roomParam != null ? Longs.tryParse(roomParam) : null;
                Room room = roomId != null ? roomManager.getRoomInstance(roomId) : null;
                if (room != null) {
                    response.jsonContent(pollService.getActivePoll(room));
                    return;
                }
            } else if (request.method() == HttpMethod.POST) {
                String action = request.postParam("action");
                if (action.equals("CREATE")) {
                    String questionParam = request.postParam("question");
                    List<String> options = request.postParams("option");

                    String roomParam = request.postParam("room");
                    Long roomId = roomParam != null ? Longs.tryParse(roomParam) : null;
                    Room room = roomId != null ? roomManager.getRoomInstance(roomId) : null;
                    if (room != null && questionParam != null && options.size() > 1 && options.size() <= 5) {
                        pollService.createPoll(room, questionParam, options);
                        response.stringContent("OK");
                        return;
                    }
                } else if (action.equals("CLOSE")) {
                    String roomParam = request.postParam("room");
                    Long roomId = roomParam != null ? Longs.tryParse(roomParam) : null;
                    Room room = roomId != null ? roomManager.getRoomInstance(roomId) : null;
                    if (room != null) {
                        pollService.closePoll(room);
                        response.stringContent("OK");
                        return;
                    }
                }
            }
        }
        response.badRequest();
    }
}
