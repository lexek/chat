package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.db.UserAuthDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.ReCaptcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static lexek.wschat.util.Names.PASSWORD_PATTERN;
import static lexek.wschat.util.Names.USERNAME_PATTERN;

public class SetupProfileHandler extends SimpleHttpHandler {
    private final Logger logger = LoggerFactory.getLogger(RegistrationHandler.class);
    private final AuthenticationManager authenticationManager;
    private final ReCaptcha reCaptcha;

    public SetupProfileHandler(AuthenticationManager authenticationManager, ReCaptcha reCaptcha) {
        this.authenticationManager = authenticationManager;
        this.reCaptcha = reCaptcha;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        String ip = request.ip();
        boolean captchaRequired = authenticationManager.failedLoginTries(ip) > 10;
        if (request.method() == HttpMethod.GET) {
            response.renderTemplate("setup_profile",
                    ImmutableMap.of("captchaRequired", captchaRequired));
            return;
        } else if (request.method() == HttpMethod.POST) {
            UserAuthDto auth = authenticationManager.checkAuthentication(request);
            if (auth != null && auth.getUser() == null) {
                String username = request.postParam("username");
                String newAccountParam = request.queryParam("newAccount");
                boolean newAccount = newAccountParam != null && newAccountParam.equals("true");
                if (username != null) {
                    if (newAccount) {
                        username = username.toLowerCase();
                        if (USERNAME_PATTERN.matcher(username).matches()) {
                            if (authenticationManager.addUserAndTieToAuth(username, auth)) {
                                response.renderTemplate("auth", null);
                                return;
                            } else {
                                response.renderTemplate("setup_profile",
                                        ImmutableMap.of(
                                                "new_account_error", "This name is already taken.",
                                                "captchaRequired", captchaRequired
                                        ));
                                return;
                            }
                        }
                    } else {
                        String password = request.postParam("password");
                        if (password != null &&
                                USERNAME_PATTERN.matcher(username).matches() &&
                                PASSWORD_PATTERN.matcher(password).matches()) {
                            boolean captchaValid = false;
                            if (captchaRequired) {
                                String captchaResponse = request.postParam("g-recaptcha-response");
                                if (captchaResponse != null) {
                                    captchaValid = reCaptcha.verify(captchaResponse, ip);
                                }
                            } else {
                                captchaValid = true;
                            }

                            if (captchaValid) {
                                UserAuthDto userAuth = authenticationManager.fastAuth(username, password, ip);
                                if (userAuth == null) {
                                    response.renderTemplate("setup_profile",
                                            ImmutableMap.of(
                                                    "login_error", "Bas username or password.",
                                                    "captchaRequired", captchaRequired
                                            ));
                                    return;
                                } else {
                                    if (authenticationManager.tieUserWithExistingAuth(userAuth.getUser(), auth)) {
                                        response.renderTemplate("auth", null);
                                        return;
                                    } else {
                                        response.renderTemplate("setup_profile",
                                                ImmutableMap.of(
                                                        "login_error", "Couldn't tie this account.",
                                                        "captchaRequired", captchaRequired
                                                ));
                                        return;
                                    }
                                }
                            } else {
                                response.renderTemplate("setup_profile",
                                        ImmutableMap.of("login_error", "Invalid captcha.", "captchaRequired", true));
                                return;
                            }
                        }
                    }
                }
                logger.debug("", username, request.ip());
            }
        }
        response.badRequest();
    }
}
