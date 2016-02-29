package lexek.wschat.frontend.http.rest;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.EmailForm;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/email")
@RequiredRole(GlobalRole.USER_UNCONFIRMED)
public class EmailResource {
    private final AuthenticationManager authenticationManager;

    public EmailResource(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    private static boolean hasPendingVerification(UserDto user) {
        return user.getEmail() != null && !user.isEmailVerified();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setEmail(
        @Auth UserDto user,
        @Valid @NotNull EmailForm form
    ) {
        authenticationManager.setEmail(user, form.getEmail().trim().toLowerCase());
        return Response.ok().build();
    }

    @POST
    @Path("/resendVerification")
    public void resendVerificationEmail(@Auth UserDto user) {
        if (hasPendingVerification(user)) {
            authenticationManager.resendVerificationEmail(user);
        }
    }

    @GET
    @Path("/verify")
    @RequiredRole(GlobalRole.UNAUTHENTICATED)
    @Produces(MediaType.TEXT_PLAIN)
    public String verifyEmail(
        @QueryParam("code") String code,
        @QueryParam("uid") long userId
    ) {
        boolean success = this.authenticationManager.verifyEmail(code, userId);
        if (success) {
            return "your email is successfully verified";
        }
        return "there was an error with verifying your email";
    }
}
