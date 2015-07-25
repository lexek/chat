package lexek.wschat.frontend.http.rest;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.GlobalRole;
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
import java.util.Map;

@Path("/email")
@RequiredRole(GlobalRole.USER)
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
    public Response setEmail(
        @Auth UserDto user,
        @Valid @NotNull EmailForm form
    ) {
        if (hasPendingVerification(user) || user.getEmail().equals(form.getEmail())) {
            throw new WebApplicationException(400);
        }
        authenticationManager.setEmail(user, form.getEmail().trim());
        return Response.ok().build();
    }

    @POST
    @Path("/resendVerification")
    @RequiredRole(GlobalRole.USER_UNCONFIRMED)
    public void resendVerificationEmail(@Auth UserDto user) {
        if (hasPendingVerification(user)) {
            authenticationManager.resendVerificationEmail(user);
        }
    }

    @GET
    @Path("/hasPendingVerification")
    @RequiredRole(GlobalRole.USER_UNCONFIRMED)
    @Produces(MediaType.APPLICATION_JSON)
    public Map getPendingVerification(
        @Auth UserDto user
    ) {
        return ImmutableMap.of("value", hasPendingVerification(user));
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
