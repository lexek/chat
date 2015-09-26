package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.util.Constants;

public class ChatHomeHandler extends SimpleHttpHandler {
    private final String title;
    private final boolean allowLikes;
    private final boolean singleRoom;

    public ChatHomeHandler(String title, boolean allowLikes, boolean singleRoom) {
        this.title = title;
        this.allowLikes = allowLikes;
        this.singleRoom = singleRoom;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        boolean debug = request.queryParamAsBoolean("debug");
        String room = request.queryParam("room");
        response.renderTemplate("chat", ImmutableMap.builder()
                .put("title", title)
                .put("like", allowLikes)
                .put("singleRoom", singleRoom)
                .put("debug", debug)
                .put("room", room != null ? "#" + room : "#main")
                .put("protocolVersion", Constants.WEBSOCKET_PROTOCOL_VERSION)
                .build()
        );
    }
}
