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

import java.util.Set;

import static lexek.wschat.util.Names.*;

public class RegistrationHandler extends SimpleHttpHandler {
    private final Logger logger = LoggerFactory.getLogger(RegistrationHandler.class);
    private final ReCaptcha reCaptcha;
    private final AuthenticationManager authenticationManager;
    private final Set<String> bannedIps;

    public RegistrationHandler(AuthenticationManager authenticationManager, ReCaptcha reCaptcha, Set<String> bannedIps) {
        this.authenticationManager = authenticationManager;
        this.reCaptcha = reCaptcha;
        this.bannedIps = bannedIps;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (request.method() == HttpMethod.POST) {
            String username = request.postParam("username");
            String password = request.postParam("password");
            String email = request.postParam("email");
            String captchaResponse = request.postParam("g-recaptcha-response");
            if (bannedIps.contains(request.ip())) {
                response.jsonContent(ImmutableMap.of("success", false, "error", "You can't register new accounts."));
                return;
            } else if ((captchaResponse != null) && (username != null) && (password != null) && email != null) {
                email = email.trim();
                username = username.toLowerCase();
                if (USERNAME_PATTERN.matcher(username).matches() && PASSWORD_PATTERN.matcher(password).matches() && EMAIL.matcher(email).matches()) {
                    if (reCaptcha.verify(captchaResponse, request.ip())) {
                        password = BCrypt.hashpw(password, BCrypt.gensalt());
                        if (authenticationManager.registerWithPassword(username, password, email)) {
                            logger.debug("registered user {}", username);
                            response.jsonContent(ImmutableMap.of("success", true));
                        } else {
                            response.jsonContent(ImmutableMap.of("success", false, "error", "This name is already taken."));
                        }
                        return;
                    } else {
                        response.jsonContent(ImmutableMap.of("success", false, "error", "Wrong captcha."));
                        logger.debug("failed captcha {}", username);
                        return;
                    }
                }
            }
            response.jsonContent(ImmutableMap.of("success", false, "error", "Bad username, email or password format."));
            logger.debug("failed to register user {}", username);
        } else {
            response.badRequest();
        }
    }
}
