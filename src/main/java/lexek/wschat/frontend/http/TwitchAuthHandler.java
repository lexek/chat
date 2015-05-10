package lexek.wschat.frontend.http;

import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.db.SessionDto;
import lexek.wschat.db.UserAuthDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.social.SocialAuthProfile;
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
        UserAuthDto auth = authenticationManager.checkAuthentication(request);
        if (auth != null) {
            response.redirect("/setup_profile");
        } else if (request.queryParamKeys().isEmpty()) {
            response.redirect(authService.getRedirectUrl());
        } else {
            try {
                String code = request.queryParam("code");
                if (code != null) {
                    String token = authService.authenticate(code);
                    SocialAuthProfile profile = authService.getProfile(token);
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
