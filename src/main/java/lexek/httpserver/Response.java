package lexek.httpserver;

import com.google.gson.JsonObject;
import freemarker.template.TemplateException;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lexek.wschat.db.SessionDto;

import java.io.IOException;
import java.io.StringWriter;

public class Response {
    private static final int COOKIE_MAX_AGE = 2592000;
    private final FullHttpResponse wrappedResponse;
    private final ViewResolvers viewResolvers;

    public Response(FullHttpResponse wrappedResponse, ViewResolvers viewResolvers) {
        this.wrappedResponse = wrappedResponse;
        this.viewResolvers = viewResolvers;
    }

    public void renderTemplate(String template, Object data) throws IOException, TemplateException {
        StringWriter stringWriter = new StringWriter();
        viewResolvers.getTemplateEngine().getTemplate(template + ".ftl").process(data, stringWriter);
        stringContent(stringWriter.toString(), "text/html; charset=UTF-8");
    }

    public void stringContent(String content) {
        stringContent(content, "text/plain; charset=UTF-8");
    }

    public void stringContent(String content, String mimeType) {
        if (content != null) {
            wrappedResponse.content().writeBytes(content.getBytes(CharsetUtil.UTF_8));
            header(HttpHeaders.Names.CONTENT_TYPE, mimeType);
        }
    }

    public void jsonContent(Object jsonObject) {
        if (jsonObject == null) {
            jsonObject = new JsonObject();
        }
        stringContent(viewResolvers.getGson().toJson(jsonObject), "application/json; charset=utf-8");
    }

    public void header(String key, String value) {
        wrappedResponse.headers().add(key, value);
    }

    public void cookie(Cookie cookie) {
        header(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(cookie));
    }

    public void notFound() {
        wrappedResponse.setStatus(HttpResponseStatus.NOT_FOUND);
        stringContent("File not found.");
    }

    public void forbidden() {
        wrappedResponse.setStatus(HttpResponseStatus.FORBIDDEN);
        stringContent("Forbidden.");
    }

    public void badRequest() {
        wrappedResponse.setStatus(HttpResponseStatus.BAD_REQUEST);
        stringContent("Bad request.");
    }

    public void badRequest(String message) {
        wrappedResponse.setStatus(HttpResponseStatus.BAD_REQUEST);
        stringContent(message);
    }

    public void redirect(String location) {
        wrappedResponse.setStatus(HttpResponseStatus.FOUND);
        header("Location", location);
    }

    public void internalError() {
        wrappedResponse.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        stringContent("A wild error appears, try again.");
    }

    public void setSessionCookie(SessionDto session) {
        Cookie cookie = new DefaultCookie("sid", session.getSessionId());
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setSecure(true);
        cookie.setPath("/");
        this.cookie(cookie);
    }
}
