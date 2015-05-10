package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Longs;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.security.CaptchaService;
import lexek.wschat.security.ReCaptcha;

public class RecaptchaHandler extends SimpleHttpHandler {
    private final CaptchaService captchaService;
    private final ReCaptcha reCaptcha;

    public RecaptchaHandler(CaptchaService captchaService, ReCaptcha reCaptcha) {
        this.captchaService = captchaService;
        this.reCaptcha = reCaptcha;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        Long id = Longs.tryParse(request.uri().substring("/recaptcha/".length()));
        if (id != null && captchaService.isValidId(id)) {
            if (request.method() == HttpMethod.GET) {
                response.renderTemplate("recaptcha", ImmutableMap.of("id", id));
            } else {
                String reCaptchaResponse = request.postParam("g-recaptcha-response");
                if ((reCaptchaResponse != null)) {
                    if (reCaptcha.verify(reCaptchaResponse, request.ip())) {
                        captchaService.success(id);
                        response.stringContent("OK");
                    } else {
                        response.stringContent("Wrong captcha.");
                    }
                } else {
                    response.stringContent("FAIL");
                }
            }
        } else {
            response.notFound();
        }
    }
}
