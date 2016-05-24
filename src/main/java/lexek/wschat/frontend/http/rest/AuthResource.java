package lexek.wschat.frontend.http.rest;

import com.google.common.collect.ImmutableMap;
import lexek.httpserver.Request;
import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.e.InvalidStateException;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.ReCaptcha;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.RequiredRole;
import lexek.wschat.security.social.*;
import org.glassfish.jersey.server.mvc.Viewable;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static lexek.wschat.util.Names.PASSWORD_PATTERN;
import static lexek.wschat.util.Names.USERNAME_PATTERN;

@Path("/auth")
public class AuthResource {
    private static final int COOKIE_MAX_AGE = 2592000;

    private final SocialAuthService socialAuthService;
    private final AuthenticationManager authenticationManager;
    private final ReCaptcha reCaptcha;

    public AuthResource(SocialAuthService socialAuthService, AuthenticationManager authenticationManager, ReCaptcha reCaptcha) {
        this.socialAuthService = socialAuthService;
        this.authenticationManager = authenticationManager;
        this.reCaptcha = reCaptcha;
    }

    @Path("/social/{serviceName}")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response socialAuth(
        @Context Request request,
        @PathParam("serviceName") @NotEmpty String provider,
        @QueryParam("code") String code,
        @QueryParam("oauth_token") String oauthToken,
        @QueryParam("oauth_verifier") String oauthVerifier,
        @QueryParam("error") String error,
        @QueryParam("error_description") String errorDescription,
        @QueryParam("state") String state,
        @CookieParam("social_state") String cookieState,
        @Auth UserDto user
    ) throws IOException {
        SocialAuthProvider socialAuthProvider = socialAuthService.getAuthService(provider);
        if (socialAuthProvider == null) {
            throw new EntityNotFoundException("Auth provider not found.");
        }

        if (errorDescription != null) {
            throw new InvalidStateException(errorDescription);
        }
        if (error != null) {
            throw new InvalidStateException(error);
        }

        SocialToken token = null;
        if (socialAuthProvider.isV1()) {
            if (oauthToken != null && oauthVerifier != null) {
                if (!oauthToken.equals(cookieState)) {
                    throw new InvalidStateException("State mismatch");
                }
                token = socialAuthProvider.authenticate(oauthToken, oauthVerifier);
            }
        } else {
            if (code != null) {
                if (state != null && cookieState != null && !state.equals(cookieState)) {
                    throw new InvalidStateException("State mismatch");
                }
                token = socialAuthProvider.authenticate(code);
            }
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
                throw new InvalidStateException("Your account must have verified email");
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
                        UserAuthDto userAuth = authenticationManager.createUserWithProfile(username, profile);
                        SessionDto sessionDto = authenticationManager.createSession(userAuth.getUser(), request.ip());
                        socialAuthService.expireTemporarySession(tempSessionId);
                        return Response
                            .ok(new Viewable("/auth"))
                            .cookie(sessionCookie("sid", sessionDto.getSessionId()))
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
                            UserAuthDto newAuth =
                                authenticationManager.createUserAuthFromProfile(user, profile);
                            SessionDto sessionDto = authenticationManager.createSession(newAuth.getUser(), ip);
                            socialAuthService.expireTemporarySession(tempSessionId);
                            return Response
                                .ok(new Viewable("/auth"))
                                .cookie(sessionCookie("sid", sessionDto.getSessionId()))
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
    @RequiredRole(GlobalRole.USER_UNCONFIRMED)
    public Response deleteAuth(
        @PathParam("serviceName") @NotEmpty String serviceName,
        @Auth UserDto user
    ) {
        serviceName = serviceName.toLowerCase().trim();
        if (serviceName.equals("password") || serviceName.equals("token")) {
            authenticationManager.deleteAuth(user, serviceName);
        } else {
            socialAuthService.deleteAuth(user, serviceName);
        }
        return Response.ok().build();
    }

    private static NewCookie sessionCookie(String name, String id) {
        return new NewCookie(name, id, "/", null, null, COOKIE_MAX_AGE, true);
    }
}
