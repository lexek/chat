package lexek.wschat.frontend.http.rest;

import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.EmailForm;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/email")
@RequiredRole(GlobalRole.USER)
public class EmailResource {
    private final AuthenticationManager authenticationManager;

    public EmailResource(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setEmail(
        @Auth UserDto user,
        @Valid @NotNull EmailForm form
    ) {
        authenticationManager.setEmail(user, form.getEmail());
        return Response.ok().build();
    }
}
