package lexek.wschat.frontend.http.admin;

import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.dao.StatisticsDao;
import lexek.wschat.security.AuthenticationManager;

public class ActivityHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final StatisticsDao statisticsDao;

    public ActivityHandler(AuthenticationManager authenticationManager, StatisticsDao statisticsDao) {
        this.authenticationManager = authenticationManager;
        this.statisticsDao = statisticsDao;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (authenticationManager.hasRole(request, GlobalRole.ADMIN)) {
            Long userId = request.queryParamAsLong("userId");
            if (userId != null) {
                response.jsonContent(statisticsDao.getUserActivity(userId));
                return;
            }
            response.badRequest();
        } else {
            response.forbidden();
        }
    }
}
