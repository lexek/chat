package lexek.httpserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import freemarker.template.TemplateException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;
import lexek.wschat.db.model.SessionDto;

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

    ByteBuf getBuffer() {
        return wrappedResponse.content();
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
        try {
            stringContent(
                viewResolvers
                    .getObjectMapper()
                    .writeValueAsString(jsonObject != null ? jsonObject : JsonNodeFactory.instance.objectNode()),
                "application/json; charset=utf-8"
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void header(String key, String value) {
        wrappedResponse.headers().add(key, value);
    }

    public void cookie(Cookie cookie) {
        header(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
    }

    public void status(int status) {
        wrappedResponse.setStatus(HttpResponseStatus.valueOf(status));
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
        badRequest("Bad request.");
    }

    public void badRequest(String message) {
        wrappedResponse.setStatus(HttpResponseStatus.BAD_REQUEST);
        try {
            renderTemplate("server_message", new ServerMessage(HttpResponseStatus.BAD_REQUEST, message));
        } catch (Exception e) {
            e.printStackTrace();
            stringContent(message);
        }
    }

    public void redirect(String location) {
        wrappedResponse.setStatus(HttpResponseStatus.FOUND);
        header("Location", location);
    }

    public void internalError() {
        internalError("A wild error appears, try again.");
    }

    public void internalError(String message) {
        wrappedResponse.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        try {
            renderTemplate("server_message", new ServerMessage(HttpResponseStatus.INTERNAL_SERVER_ERROR, message));
        } catch (Exception e) {
            e.printStackTrace();
            stringContent(message);
        }
    }

    public void setSessionCookie(SessionDto session) {
        Cookie cookie = new DefaultCookie("sid", session.getSessionId());
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setSecure(true);
        cookie.setPath("/");
        this.cookie(cookie);
    }
}
