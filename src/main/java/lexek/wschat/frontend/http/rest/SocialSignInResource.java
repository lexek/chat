package lexek.wschat.frontend.http.rest;

import com.google.common.collect.ImmutableMap;
import lexek.httpserver.Request;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.rest.ErrorModel;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.ReCaptcha;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.social.*;
import org.glassfish.jersey.server.mvc.Viewable;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static lexek.wschat.util.Names.PASSWORD_PATTERN;
import static lexek.wschat.util.Names.USERNAME_PATTERN;

@Path("/sign-in/social")
public class SocialSignInResource {
    private static final int COOKIE_MAX_AGE = 2592000;

    private final Logger logger = LoggerFactory.getLogger(SocialSignInResource.class);
    private final SocialAuthService socialAuthService;
    private final AuthenticationManager authenticationManager;
    private final ReCaptcha reCaptcha;

    public SocialSignInResource(SocialAuthService socialAuthService, AuthenticationManager authenticationManager, ReCaptcha reCaptcha) {
        this.socialAuthService = socialAuthService;
        this.authenticationManager = authenticationManager;
        this.reCaptcha = reCaptcha;
    }

    @Path("/{serviceName}")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response socialAuth(
        @Context Request request,
        @PathParam("serviceName") @NotEmpty String provider,
        @QueryParam("code") String code,
        @QueryParam("oauth_token") String oauthToken,
        @QueryParam("oauth_verifier") String oauthVerifier,
        @QueryParam("error") String error,
        @QueryParam("state") String state,
        @CookieParam("social_state") String cookieState,
        @Auth UserDto user
    ) throws IOException {
        SocialAuthProvider socialAuthProvider = socialAuthService.getAuthService(provider);
        if (socialAuthProvider == null) {
            return Response.status(404).entity(ImmutableMap.of("error", "not found")).build();
        }

        if (error != null) {
            return Response.status(500).entity(ImmutableMap.of("error", error)).build();
        }

        SocialToken token = null;
        if (socialAuthProvider.isV1()) {
            if (oauthToken != null && oauthVerifier != null) {
                if (!oauthToken.equals(cookieState)) {
                    return Response.status(400).entity(ImmutableMap.of("error", "state mismatch")).build();
                }
                token = socialAuthProvider.authenticate(oauthToken, oauthVerifier);
            }
        } else {
            if (code != null) {
                if (state != null && cookieState != null && !state.equals(cookieState)) {
                    return Response.status(400).entity(ImmutableMap.of("error", "state mismatch")).build();
                }
                token = socialAuthProvider.authenticate(code);
            }
        }

        if (token != null) {
            SocialProfile profile = socialAuthProvider.getProfile(token);
            //todo: register or add to account
            if (profile.getEmail() != null || !socialAuthProvider.checkEmail()) {
                //do authentication/registration
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
                            .header("Location", "/rest/sign-in/social/setup_profile")
                            .cookie(sessionCookie)
                            .build();
                    }
                } else {
                    UserAuthDto userAuthDto = authenticationManager.getOrCreateUserAuth(profile, true);
                    authenticationManager.tieUserWithExistingAuth(user, userAuthDto);
                    return Response
                        .ok(new Viewable("/auth", null))
                        .build();
                }
            } else {
                throw new WebApplicationException(Response
                    .status(403)
                    .entity(new ErrorModel("Your account must have verified email"))
                    .build());
            }
            return Response.ok(ImmutableMap.of("success", true)).build();
        }
        SocialRedirect socialRedirect = socialAuthProvider.getRedirect();
        NewCookie stateCookie = new NewCookie("social_state", socialRedirect.getState());
        return Response.status(302).header("Location", socialRedirect.getUrl()).cookie(stateCookie).build();
    }

    @Path("/setup_profile")
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

    @Path("/setup_profile")
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
            if (newAccount) {
                if (USERNAME_PATTERN.matcher(username).matches()) {
                    try {
                        UserAuthDto userAuth = authenticationManager.createUserWithProfile(username, profile);
                        SessionDto sessionDto = authenticationManager.createSession(userAuth, request.ip());
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
                        UserAuthDto userAuth = authenticationManager.fastAuth(username, password, ip);
                        if (userAuth == null) {
                            return Response.ok(new Viewable(
                                "/setup_profile",
                                ImmutableMap.of(
                                    "login_error", "Bas username or password.",
                                    "captchaRequired", captchaRequired
                                ))).build();
                        } else {
                            UserAuthDto newAuth =
                                authenticationManager.createUserAuthFromProfile(userAuth.getUser(), profile);
                            SessionDto sessionDto = authenticationManager.createSession(newAuth, ip);
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

    private static NewCookie sessionCookie(String name, String id) {
        return new NewCookie(name, id, "/", null, null, COOKIE_MAX_AGE, true);
    }
}
