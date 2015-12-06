package lexek.httpserver;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.UserDto;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class JerseySecurityContext implements SecurityContext {
    private final UserDto userDto;

    public JerseySecurityContext(UserDto userDto) {
        this.userDto = userDto;
    }

    @Override
    public Principal getUserPrincipal() {
        return userDto;
    }

    @Override
    public boolean isUserInRole(String role) {
        return userDto.hasRole(GlobalRole.valueOf(role));
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String getAuthenticationScheme() {
        return "SID";
    }
}
