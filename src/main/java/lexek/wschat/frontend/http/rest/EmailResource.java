package lexek.wschat.frontend.http.rest;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.EmailForm;
import lexek.wschat.db.model.rest.ErrorModel;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response setEmail(
        @Auth UserDto user,
        @Valid @NotNull EmailForm form
    ) {
        if (hasPendingVerification(user)) {
            return Response
                .status(400)
                .entity(new ErrorModel("You cannot change email until you verify this one."))
                .build();
        }
        if (user.getEmail().equals(form.getEmail())) {
            return Response
                .status(400)
                .entity(new ErrorModel("The new email is same as old."))
                .build();
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
