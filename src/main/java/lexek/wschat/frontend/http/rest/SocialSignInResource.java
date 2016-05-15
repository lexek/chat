package lexek.wschat.frontend.http.rest;

import com.google.common.collect.ImmutableMap;
import lexek.httpserver.Request;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.rest.ErrorModel;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.social.*;
import org.glassfish.jersey.server.mvc.Viewable;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/sign-in/social")
public class SocialSignInResource {
    private static final int COOKIE_MAX_AGE = 2592000;

    private final SocialAuthService socialAuthService;
    private final AuthenticationManager authenticationManager;

    public SocialSignInResource(SocialAuthService socialAuthService, AuthenticationManager authenticationManager) {
        this.socialAuthService = socialAuthService;
        this.authenticationManager = authenticationManager;
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
                    UserAuthDto userAccount = authenticationManager.getOrCreateUserAuth(profile);
                    SessionDto session = authenticationManager.createSession(userAccount, request.ip(), System.currentTimeMillis());
                    if (session != null) {
                        NewCookie sessionCookie =
                            new NewCookie("sid", session.getSessionId(), "/", null, null, COOKIE_MAX_AGE, true);
                        if (session.getUserAuth().getUser() != null) {
                            return Response
                                .ok(new Viewable("/auth", null))
                                .cookie(sessionCookie)
                                .build();
                        } else {
                            return Response
                                .status(302)
                                .header("Location", "/rest/sign-in/social/setup_profile")
                                .cookie(sessionCookie)
                                .build();
                        }
                    }
                } else {
                    UserAuthDto userAuthDto = authenticationManager.getOrCreateUserAuth(profile);
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
        UserAuthDto userAuth = authenticationManager.checkFullAuthentication(
            request.cookieValue("sid"),
            request.ip()
        );
        if (userAuth == null || userAuth.getUser() != null) {
            throw new WebApplicationException(403);
        }
        String ip = request.ip();
        boolean captchaRequired = authenticationManager.failedLoginTries(ip) > 10;
        return new Viewable("/setup_profile", ImmutableMap.of("captchaRequired", captchaRequired));
    }

    @Path("/setup_profile")
    @POST
    public Viewable setupProfilePost(@Context Request request) {
        UserAuthDto userAuth = authenticationManager.checkFullAuthentication(
            request.cookieValue("sid"),
            request.ip()
        );
        if (userAuth == null || userAuth.getUser() != null) {
            throw new WebApplicationException(403);
        }
        //todo: do setup profile
        String ip = request.ip();
        boolean captchaRequired = authenticationManager.failedLoginTries(ip) > 10;
        return new Viewable("/setup_profile", ImmutableMap.of("captchaRequired", captchaRequired));
    }
}
