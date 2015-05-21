package lexek.wschat.frontend.http.admin;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Longs;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.jooq.tables.records.UserRecord;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.UserService;
import org.jooq.TableField;
import org.jooq.exception.DataAccessException;

import java.util.Map;

import static lexek.wschat.db.jooq.tables.User.USER;

public class UserApiHandler extends SimpleHttpHandler {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public UserApiHandler(AuthenticationManager authenticationManager, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        UserAuthDto auth = authenticationManager.checkFullAuthentication(request);
        if (auth != null && auth.getUser() != null && auth.getUser().getRole().compareTo(GlobalRole.ADMIN) >= 0) {
            if (request.method() == HttpMethod.GET) {
                String idParam = request.queryParam("id");
                Long id = idParam != null ? Longs.tryParse(idParam) : null;
                if (id != null) {
                    response.jsonContent(userService.fetchData(id));
                    return;
                }
            } else if (request.method() == HttpMethod.POST) {
                String actionParam = request.postParam("action");
                String idParam = request.postParam("id");
                Long id = idParam != null ? Longs.tryParse(idParam) : null;

                if (id != null && actionParam != null) {
                    if (actionParam.equals("DELETE")) {
                        UserDto user = userService.fetchById(id);
                        UserDto admin = auth.getUser();
                        if (user != null && user.getRole() == GlobalRole.USER && admin.getRole() == GlobalRole.SUPERADMIN) {
                            userService.delete(user, admin);
                            return;
                        }
                    } else if (actionParam.equals("UPDATE")) {
                        UserDto user = userService.fetchById(id);
                        UserDto admin = auth.getUser();
                        if (user != null) {
                            handleUpdate(request, response, user, admin);
                            return;
                        }
                    }
                }
            }
        }
        response.badRequest();
    }

    private void handleUpdate(Request request, Response response, final UserDto user, UserDto admin) {
        ImmutableMap.Builder<TableField<UserRecord, ?>, Object> mapBuilder = ImmutableMap.builder();
        String renameParam = request.postParam("rename");
        if (renameParam != null) {
            mapBuilder.put(USER.RENAME_AVAILABLE, Boolean.parseBoolean(renameParam));
        }
        String bannedParam = request.postParam("banned");
        if (bannedParam != null) {
            if (user.getRole() == GlobalRole.USER) {
                boolean value = Boolean.parseBoolean(bannedParam);
                mapBuilder.put(USER.BANNED, value);
            }
        }
        String roleParam = request.postParam("role");
        if (roleParam != null) {
            GlobalRole role = GlobalRole.valueOf(roleParam);
            if (admin.getRole().compareTo(user.getRole()) > 0 && admin.getRole().compareTo(role) > 0) {
                mapBuilder.put(USER.ROLE, role);
            }
        }
        String nameParam = request.postParam("name");
        if (nameParam != null && admin.getRole() == GlobalRole.SUPERADMIN && user.getRole() == GlobalRole.USER) {
            mapBuilder.put(USER.NAME, nameParam);
        }
        Map<TableField<UserRecord, ?>, Object> map = mapBuilder.build();
        if (map.size() != 0) {
            try {
                userService.update(user, admin, map);
                response.stringContent("ok");
            } catch (DataAccessException e) {
                response.badRequest("this name is already occupied");
            }
        } else {
            response.badRequest();
        }
    }
}
