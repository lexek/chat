package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.db.model.SessionDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.ReCaptcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static lexek.wschat.util.Names.PASSWORD_PATTERN;
import static lexek.wschat.util.Names.USERNAME_PATTERN;

public class LoginHandler extends SimpleHttpHandler {
    private static final int COOKIE_MAX_AGE = 2592000;

    private final Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    private final AuthenticationManager authenticationManager;
    private final ReCaptcha reCaptcha;

    public LoginHandler(AuthenticationManager authenticationManager, ReCaptcha reCaptcha) {
        this.authenticationManager = authenticationManager;
        this.reCaptcha = reCaptcha;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        String ip = request.ip();
        if (request.method() == HttpMethod.GET) {
            response.jsonContent(ImmutableMap.of("captchaRequired", authenticationManager.failedLoginTries(ip) > 10));
        } else if (request.method() == HttpMethod.POST) {
            String username = request.postParam("username");
            String password = request.postParam("password");

            boolean captchaValid = false;
            if (authenticationManager.failedLoginTries(ip) > 10) {
                String captchaResponse = request.postParam("g-recaptcha-response");
                if (captchaResponse != null) {
                    captchaValid = reCaptcha.verify(captchaResponse, ip);
                }
            } else {
                captchaValid = true;
            }

            if (!captchaValid) {
                response.jsonContent(ImmutableMap.of("success", false, "error", "Wrong captcha.", "captchaRequired", true));
                return;
            }

            if (username != null && password != null) {
                username = username.toLowerCase();
                if (USERNAME_PATTERN.matcher(username).matches() && PASSWORD_PATTERN.matcher(password).matches()) {
                    SessionDto session = authenticationManager.authenticate(username, password, ip, System.currentTimeMillis());
                    if (session != null) {
                        Cookie cookie = new DefaultCookie("sid", session.getSessionId());
                        cookie.setMaxAge(COOKIE_MAX_AGE);
                        cookie.setPath("/");
                        cookie.setSecure(true);
                        response.cookie(cookie);
                        logger.debug("{} logged in with sid {}", username, session.getSessionId());
                        response.jsonContent(ImmutableMap.of("success", true));
                        return;
                    }
                }
            }
            logger.debug("Wrong username or password:{}@{}", username, request.ip());
            response.jsonContent(ImmutableMap.of("success", false, "error", "Wrong user name or password.",
                "captchaRequired", authenticationManager.failedLoginTries(ip) > 10));
        } else {
            response.stringContent("This resource accepts only POST requests.");
        }
    }
}
