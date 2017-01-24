package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.ReCaptcha;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static lexek.wschat.util.Names.*;

public class RegistrationHandler extends SimpleHttpHandler {
    private final Logger logger = LoggerFactory.getLogger(RegistrationHandler.class);
    private final ReCaptcha reCaptcha;
    private final AuthenticationManager authenticationManager;

    public RegistrationHandler(AuthenticationManager authenticationManager, ReCaptcha reCaptcha) {
        this.authenticationManager = authenticationManager;
        this.reCaptcha = reCaptcha;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (request.method() == HttpMethod.POST) {
            String username = request.postParam("username");
            String password = request.postParam("password");
            String email = request.postParam("email");
            String captchaResponse = request.postParam("g-recaptcha-response");
            if (authenticationManager.getBannedIps().contains(request.ip())) {
                response.jsonContent(ImmutableMap.of("success", false, "error", "REGISTRATION_DENIED"));
                return;
            } else if ((captchaResponse != null) && (username != null) && (password != null) && email != null) {
                email = email.trim();
                username = username.toLowerCase();
                if (USERNAME_PATTERN.matcher(username).matches() && PASSWORD_PATTERN.matcher(password).matches() && EMAIL.matcher(email).matches()) {
                    if (reCaptcha.verify(captchaResponse, request.ip())) {
                        password = BCrypt.hashpw(password, BCrypt.gensalt());
                        try {
                            authenticationManager.registerWithPassword(username, password, email);
                            logger.debug("registered user {}", username);
                            response.jsonContent(ImmutableMap.of("success", true));
                        } catch (Exception e) {
                            logger.warn("caught exception", e);
                            response.jsonContent(ImmutableMap.of("success", false, "error", e.getMessage()));
                        }
                        return;
                    } else {
                        response.jsonContent(ImmutableMap.of("success", false, "error", "WRONG_CAPTCHA"));
                        logger.debug("failed captcha {}", username);
                        return;
                    }
                }
            }
            response.jsonContent(ImmutableMap.of("success", false, "error", "BAD_REGISTRATION_FORMAT"));
            logger.debug("failed to register user {}", username);
        } else {
            response.badRequest();
        }
    }
}
