package lexek.wschat.frontend.http.rest;

import com.google.common.collect.ImmutableMap;
import lexek.httpserver.Request;
import lexek.wschat.chat.e.BadRequestException;
import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.PasswordForm;
import lexek.wschat.db.model.form.PasswordResetForm;
import lexek.wschat.db.model.rest.BooleanValueContainer;
import lexek.wschat.db.model.rest.PasswordModel;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.ReCaptcha;
import lexek.wschat.security.ResetPasswordService;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.security.social.*;
import lexek.wschat.security.social.provider.SocialAuthProvider;
import lexek.wschat.services.UserService;
import org.glassfish.jersey.server.mvc.Viewable;
import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;

import static lexek.wschat.util.Names.PASSWORD_PATTERN;
import static lexek.wschat.util.Names.USERNAME_PATTERN;

@Path("/auth")
public class AuthResource {
    private static final int COOKIE_MAX_AGE = 2592000;

    private final SocialAuthService socialAuthService;
    private final AuthenticationManager authenticationManager;
    private final ResetPasswordService resetPasswordService;
    private final UserService userService;
    private final ReCaptcha reCaptcha;

    @Inject
    public AuthResource(
        SocialAuthService socialAuthService,
        AuthenticationManager authenticationManager,
        ResetPasswordService resetPasswordService,
        UserService userService,
        ReCaptcha reCaptcha
    ) {
        this.socialAuthService = socialAuthService;
        this.authenticationManager = authenticationManager;
        this.resetPasswordService = resetPasswordService;
        this.userService = userService;
        this.reCaptcha = reCaptcha;
    }

    @Path("/social/{serviceName}")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response socialAuth(
        @Context Request request,
        @PathParam("serviceName") @NotEmpty String provider,
        @QueryParam("error") String error,
        @QueryParam("error_description") String errorDescription,
        @QueryParam("state") String state,
        @CookieParam("social_state") String cookieState,
        @Context UriInfo uriInfo,
        @Auth UserDto user
    ) throws IOException {
        SocialAuthProvider socialAuthProvider = socialAuthService.getAuthService(provider);
        if (socialAuthProvider == null) {
            throw new EntityNotFoundException("Auth provider not found.");
        }

        if (errorDescription != null) {
            throw new BadRequestException(errorDescription);
        }
        if (error != null) {
            throw new BadRequestException(error);
        }

        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        SocialToken token = null;

        if (socialAuthProvider.validateParams(parameters)) {
            if (!socialAuthProvider.validateState(parameters, cookieState)) {
                throw new BadRequestException("State mismatch");
            }
            token = socialAuthProvider.authenticate(parameters);
        }

        if (token != null) {
            SocialProfile profile = socialAuthProvider.getProfile(token);
            if (profile.getEmail() != null || !socialAuthProvider.checkEmail()) {
                if (user == null) {
                    SessionResult sessionResult = socialAuthService.getSession(profile, request.ip());
                    if (sessionResult.getType() == SocialAuthResultType.SESSION) {
                        NewCookie sessionCookie = sessionCookie("sid", sessionResult.getSessionId());
                        return Response
                            .ok(new Viewable("/auth", null))
                            .cookie(sessionCookie)
                            .build();
                    }
                    if (sessionResult.getType() == SocialAuthResultType.TEMP_SESSION) {
                        NewCookie sessionCookie = sessionCookie("temp_sid", sessionResult.getSessionId());
                        return Response
                            .status(302)
                            .header("Location", "/rest/auth/social/setup_profile")
                            .cookie(sessionCookie)
                            .build();
                    }
                } else {
                    authenticationManager.getOrCreateUserAuth(profile, user);
                    return Response
                        .ok(new Viewable("/auth", null))
                        .build();
                }
            } else {
                throw new BadRequestException("Your account must have verified email");
            }
            return Response.ok(ImmutableMap.of("success", true)).build();
        }
        SocialRedirect socialRedirect = socialAuthProvider.getRedirect();
        NewCookie stateCookie = new NewCookie("social_state", socialRedirect.getState());
        return Response.status(302).header("Location", socialRedirect.getUrl()).cookie(stateCookie).build();
    }

    @Path("/social/setup_profile")
    @GET
    public Viewable setupProfilePage(@Context Request request) {
        SocialProfile profile = socialAuthService.getTempSession(
            request.cookieValue("temp_sid"),
            request.ip()
        );
        String ip = request.ip();
        boolean captchaRequired = authenticationManager.failedLoginTries(ip) > 10;
        return new Viewable(
            "/setup_profile",
            ImmutableMap.of(
                "captchaRequired", captchaRequired,
                "profile", profile
            )
        );
    }

    @Path("/social/setup_profile")
    @Produces(MediaType.TEXT_HTML)
    @POST
    public Response setupProfilePost(
        @QueryParam("newAccount") @DefaultValue("false") boolean newAccount,
        @FormParam("username") String username,
        @FormParam("password") String password,
        @FormParam("g-recaptcha-response") String captchaValue,
        @Context Request request
    ) {
        String tempSessionId = request.cookieValue("temp_sid");
        SocialProfile profile = socialAuthService.getTempSession(tempSessionId, request.ip());
        String ip = request.ip();
        boolean captchaRequired = authenticationManager.failedLoginTries(ip) > 10;
        if (username != null) {
            username = username.toLowerCase();
            if (newAccount) {
                if (USERNAME_PATTERN.matcher(username).matches()) {
                    try {
                        SessionDto session = socialAuthService.createUser(username, profile, request.ip());
                        socialAuthService.expireTemporarySession(tempSessionId);
                        return Response
                            .ok(new Viewable("/auth"))
                            .cookie(sessionCookie("sid", session.getSessionId()))
                            .build();
                    } catch (Exception e) {
                        return Response.ok(new Viewable(
                            "/setup_profile",
                            ImmutableMap.of(
                                "new_account_error", "This name is already taken.",
                                "captchaRequired", captchaRequired
                            )
                        )).build();
                    }
                } else {
                    return Response.ok(new Viewable(
                        "/setup_profile",
                        ImmutableMap.of(
                            "new_account_error", "Incorrect name format.",
                            "captchaRequired", captchaRequired
                        )
                    )).build();
                }
            } else {
                if (password != null &&
                    USERNAME_PATTERN.matcher(username).matches() &&
                    PASSWORD_PATTERN.matcher(password).matches()) {
                    boolean captchaValid = false;
                    if (captchaRequired) {
                        if (captchaValue != null) {
                            captchaValid = reCaptcha.verify(captchaValue, ip);
                        }
                    } else {
                        captchaValid = true;
                    }

                    if (captchaValid) {
                        UserDto user = authenticationManager.fastAuth(username, password, ip);
                        if (user == null) {
                            return Response.ok(new Viewable(
                                "/setup_profile",
                                ImmutableMap.of(
                                    "login_error", "Bas username or password.",
                                    "captchaRequired", captchaRequired
                                ))).build();
                        } else {
                            SessionDto session = socialAuthService.createAuth(user, profile, request.ip());
                            socialAuthService.expireTemporarySession(tempSessionId);
                            return Response
                                .ok(new Viewable("/auth"))
                                .cookie(sessionCookie("sid", session.getSessionId()))
                                .build();
                        }
                    } else {
                        return Response.ok(new Viewable(
                            "/setup_profile",
                            ImmutableMap.of(
                                "login_error", "Invalid captcha.",
                                "captchaRequired", true
                            )
                        )).build();
                    }
                } else {
                    return Response.ok(new Viewable(
                        "/setup_profile",
                        ImmutableMap.of(
                            "login_error", "Incorrect password of name format.",
                            "captchaRequired", true
                        )
                    )).build();
                }
            }
        }
        return Response.ok(new Viewable(
            "/setup_profile",
            ImmutableMap.of(
                "captchaRequired", captchaRequired,
                "profile", profile
            )
        )).build();
    }

    @DELETE
    @Path("/{serviceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiredRole(GlobalRole.USER_UNCONFIRMED)
    public Response deleteAuth(
        @PathParam("serviceName") @NotEmpty String serviceName,
        @Auth UserDto user,
        PasswordModel passwordModel
    ) {
        String password = passwordModel != null ? passwordModel.getPassword() : null;
        serviceName = serviceName.toLowerCase().trim();
        switch (serviceName) {
            case "password":
                authenticationManager.deletePassword(user, password);
                break;
            case "token":
                authenticationManager.deleteAuth(user, serviceName);
                break;
            default:
                socialAuthService.deleteAuth(user, serviceName);
                break;
        }
        return Response.ok().build();
    }

    @PUT
    @Path("/password")
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

    @PUT
    @Path("/self/checkIp")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredRole(GlobalRole.USER_UNCONFIRMED)
    public Response deleteAuth(
        @Auth UserDto user,
        @NotNull BooleanValueContainer valueContainer
    ) {
        userService.setCheckIp(user, valueContainer.getValue());
        return Response.ok().build();
    }

    @POST
    @Path("/requestPasswordReset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredRole(GlobalRole.UNAUTHENTICATED)
    public Response requestPasswordReset(
        @Context Request request,
        @Valid PasswordResetForm form
    ) {
        if (!reCaptcha.verify(form.getCaptcha(), request.ip())) {
            throw new BadRequestException("Invalid captcha");
        }
        resetPasswordService.requestPasswordReset(form.getName());
        return Response.ok().build();
    }

    @GET
    @Path("/forgotPassword")
    @Produces(MediaType.TEXT_HTML)
    public Response passwordResetView(
        @NotEmpty @QueryParam("token") String token,
        @NotEmpty @QueryParam("uid") String uid
    ) {
        return Response.ok(new Viewable(
            "/forgot_password",
            ImmutableMap.of(
                "token", token,
                "uid", uid
            )
        )).build();
    }

    @POST
    @Path("/forgotPassword")
    @Produces(MediaType.TEXT_HTML)
    public Response passwordReset(
        @FormParam("token") @NotEmpty String token,
        @FormParam("uid") long userId,
        @FormParam("password") @NotEmpty String password
    ) {
        try {
            resetPasswordService.resetPassword(token, userId, password);
            return Response.ok(new Viewable(
                "/password_changed"
            )).build();
        } catch (BadRequestException e) {
            return Response.ok(new Viewable(
                "/forgot_password",
                ImmutableMap.of(
                    "token", token,
                    "uid", userId,
                    "password", password,
                    "error", e.getMessage()
                )
            )).build();
        }
    }

    private static NewCookie sessionCookie(String name, String id) {
        return new NewCookie(name, id, "/", null, null, COOKIE_MAX_AGE, true);
    }
}
