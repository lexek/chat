package lexek.wschat.frontend.http.rest;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.PasswordForm;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/password")
@RequiredRole(GlobalRole.USER)
public class PasswordResource {
    private final AuthenticationManager authenticationManager;

    public PasswordResource(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(
        @Auth UserDto user,
        @NotNull @Valid PasswordForm passwordForm
    ) {
        String password = passwordForm.getPassword();
        String oldPassword = passwordForm.getOldPassword();
        authenticationManager.setPassword(user, password, oldPassword);
        return Response.ok().build();
    }
}
