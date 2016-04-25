package lexek.wschat.frontend.http;

import io.netty.handler.codec.http.cookie.DefaultCookie;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.security.social.SocialRedirect;
import lexek.wschat.security.social.SocialToken;
import lexek.wschat.security.social.TwitchTvSocialAuthService;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwitchAuthHandler extends SimpleHttpHandler {
    private final Logger logger = LoggerFactory.getLogger(TwitchAuthHandler.class);
    private final AuthenticationManager authenticationManager;
    private final TwitchTvSocialAuthService authService;

    public TwitchAuthHandler(AuthenticationManager authenticationManager, TwitchTvSocialAuthService authService) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        UserAuthDto auth = authenticationManager.checkFullAuthentication(request);
        if (auth != null) {
            if (auth.getUser() != null) {
                response.renderTemplate("auth", null);
            } else {
                response.redirect("/setup_profile");
            }
        } else if (request.queryParamKeys().isEmpty()) {
            SocialRedirect redirect = authService.getRedirect();
            response.cookie(new DefaultCookie("twitch_state", redirect.getState()));
            response.redirect(redirect.getUrl());
        } else {
            if (request.queryParam("error") != null) {
                logger.info("{}", request.queryParam("error"), request.queryParam("error_description"));
                response.internalError("twitch error");
                return;
            }
            try {
                String cookieToken = request.cookieValue("twitch_state");
                if (cookieToken == null) {
                    response.badRequest("state check failed");
                    return;
                }
                String urlToken = request.queryParam("state");
                if (urlToken == null) {
                    response.badRequest("no state parameter");
                    return;
                }
                if (!cookieToken.equals(urlToken)) {
                    response.badRequest("state check failed");
                    return;
                }
                String code = request.queryParam("code");
                if (code != null) {
                    SocialToken token = authService.authenticate(code);
                    SocialProfile profile = authService.getProfile(token);
                    if (profile.getEmail() != null) {
                        //do authentication/registration
                        UserAuthDto userAccount = authenticationManager.getOrCreateUserAuth(profile);
                        SessionDto session = authenticationManager.createSession(userAccount, request.ip(), System.currentTimeMillis());
                        if (session != null) {
                            response.setSessionCookie(session);
                            if (session.getUserAuth().getUser() != null) {
                                response.renderTemplate("auth", null);
                            } else {
                                response.redirect("/setup_profile");
                            }
                        }
                    } else {
                        response.stringContent("You must have twitch account with verified email.");
                    }
                } else {
                    response.internalError();
                }
            } catch (ClientProtocolException e) {
                logger.debug("client protocol exception: {}", e.getMessage());
                response.internalError();
            } catch (Exception e) {
                logger.warn("", e);
                response.internalError();
            }
        }
    }
}
