package lexek.wschat.security.jersey;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.rest.ErrorModel;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

public class SecurityFilter implements ContainerRequestFilter {
    private final GlobalRole role;

    public SecurityFilter(GlobalRole role) {
        this.role = role;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        SecurityContext securityContext = requestContext.getSecurityContext();
        Principal principal = securityContext.getUserPrincipal();
        if (principal != null && principal instanceof UserDto) {
            if (((UserDto) principal).hasRole(role)) {
                return;
            }
        }
        requestContext.abortWith(
            Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorModel("Access denied."))
                .build());
    }
}
