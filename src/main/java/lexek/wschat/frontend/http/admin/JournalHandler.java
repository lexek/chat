package lexek.wschat.frontend.http.admin;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.dao.JournalDao;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;

public class JournalHandler extends SimpleHttpHandler {
    private static final int PAGE_LENGTH = 15;

    private final AuthenticationManager authenticationManager;
    private final JournalDao journalDao;

    public JournalHandler(AuthenticationManager authenticationManager, JournalDao journalDao) {
        this.authenticationManager = authenticationManager;
        this.journalDao = journalDao;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (request.method() == HttpMethod.GET) {
            UserDto userDto = authenticationManager.checkAuthentication(request);
            String pageParam = request.queryParam("page");
            String roomParam = request.queryParam("room");
            Integer page = pageParam != null ? Ints.tryParse(pageParam) : null;
            Long roomId = roomParam != null ? Longs.tryParse(roomParam) : null;

            if (page != null && page >= 0) {
                if (roomId != null) {
                    if (userDto != null && userDto.hasRole(GlobalRole.ADMIN)) {
                        response.jsonContent(journalDao.fetchAllForRoom(page, PAGE_LENGTH, roomId));
                        return;
                    }
                } else {
                    if (userDto != null && userDto.hasRole(GlobalRole.SUPERADMIN)) {
                        response.jsonContent(journalDao.fetchAllGlobal(page, PAGE_LENGTH));
                        return;
                    }
                }
            }
        }
        response.badRequest();
    }
}
