package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;

import java.util.Map;

public class ChatHomeHandler extends SimpleHttpHandler {
    private static final String TITLE = "Yoba chat";
    private final Map data;

    public ChatHomeHandler(boolean allowLikes, boolean singleRoom) {
        this.data = ImmutableMap.of(
            "title", TITLE,
            "like", allowLikes,
            "singleRoom", singleRoom
        );
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        response.renderTemplate("chat", data);
    }
}
