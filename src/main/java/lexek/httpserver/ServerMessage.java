package lexek.httpserver;

import io.netty.handler.codec.http.HttpResponseStatus;

public class ServerMessage {
    private final HttpResponseStatus status;
    private final String text;

    public ServerMessage(HttpResponseStatus status, String text) {
        this.status = status;
        this.text = text;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public String getText() {
        return text;
    }
}
