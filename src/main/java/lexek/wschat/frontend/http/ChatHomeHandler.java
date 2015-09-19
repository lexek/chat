package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;

public class ChatHomeHandler extends SimpleHttpHandler {
    private static final String TITLE = "Yoba chat";
    private final boolean allowLikes;
    private final boolean singleRoom;

    public ChatHomeHandler(boolean allowLikes, boolean singleRoom) {
        this.allowLikes = allowLikes;
        this.singleRoom = singleRoom;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        boolean debug = request.queryParamAsBoolean("debug");
        response.renderTemplate("chat", ImmutableMap.of(
            "title", TITLE,
            "like", allowLikes,
            "singleRoom", singleRoom,
            "debug", debug
        ));
    }
}
