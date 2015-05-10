package lexek.wschat.frontend.http.admin;

import com.google.common.primitives.Ints;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.JournalDao;
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
        if (authenticationManager.hasRole(request, GlobalRole.SUPERADMIN)) {
            if (request.method() == HttpMethod.GET) {
                handleGet(request, response);
                return;
            }
        }
        response.badRequest();
    }

    private void handleGet(Request request, Response response) {
        String pageParam = request.queryParam("page");
        Integer page = pageParam != null ? Ints.tryParse(pageParam) : null;

        if (page != null && page >= 0) {
            response.stringContent(journalDao.getAllPagedAsJson(page, PAGE_LENGTH), "application/json; charset=utf-8");
        } else {
            response.badRequest();
        }
    }
}
