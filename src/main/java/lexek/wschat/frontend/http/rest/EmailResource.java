package lexek.wschat.frontend.http.rest;

import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.EmailForm;
import lexek.wschat.security.UserEmailManager;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/email")
@RequiredRole(GlobalRole.USER_UNCONFIRMED)
public class EmailResource {
    private final UserEmailManager userEmailManager;

    @Inject
    public EmailResource(UserEmailManager userEmailManager) {
        this.userEmailManager = userEmailManager;
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
        userEmailManager.setEmail(user, form.getEmail().trim().toLowerCase());
        return Response.ok().build();
    }

    @POST
    @Path("/resendVerification")
    public void resendVerificationEmail(@Auth UserDto user) {
        if (hasPendingVerification(user)) {
            userEmailManager.resendVerificationEmail(user);
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
        if (userEmailManager.verifyEmail(code, userId)) {
            return "your email is successfully verified";
        }
        return "there was an error with verifying your email";
    }
}
