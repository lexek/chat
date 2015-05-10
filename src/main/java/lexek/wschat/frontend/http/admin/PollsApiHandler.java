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

public class PollsApiHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final RoomManager roomManager;
    private final PollService pollService;

    public PollsApiHandler(AuthenticationManager authenticationManager, RoomManager roomManager, PollService pollService) {
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
                    response.jsonContent(pollService.getOldPolls(room));
                    return;
                }
            }
        }
        response.badRequest();
    }

}
