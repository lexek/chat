package lexek.wschat.frontend.http.admin;

import com.google.common.primitives.Longs;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.Announcement;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.AnnouncementService;

public class AnnouncementApiHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final AnnouncementService announcementService;

    public AnnouncementApiHandler(AuthenticationManager authenticationManager, AnnouncementService announcementService) {
        this.authenticationManager = authenticationManager;
        this.announcementService = announcementService;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        UserDto user = authenticationManager.checkAuthentication(request);
        if (user != null && user.hasRole(GlobalRole.ADMIN)) {
            if (request.method() == HttpMethod.POST) {
                String action = request.postParam("action");
                if (action != null) {
                    if (action.equals("DELETE")) {
                        String idParam = request.postParam("id");
                        Long id = idParam != null ? Longs.tryParse(idParam) : null;
                        if (id != null) {
                            announcementService.setInactive(id, user);
                            response.stringContent("ok");
                            return;
                        }
                    } else if (action.equals("ADD")) {
                        String text = request.postParam("text");
                        String roomParam = request.postParam("room");
                        Long roomId = roomParam != null ? Longs.tryParse(roomParam) : null;
                        if (roomId != null && text != null) {
                            Announcement announcement = new Announcement(null, roomId, true, text);
                            announcementService.announce(announcement, user);
                            response.stringContent("ok");
                            return;
                        }
                    }
                }
            }
        } else {
            response.forbidden();
            return;
        }
        response.badRequest();
    }
}
