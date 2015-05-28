package lexek.wschat.proxy.goodgame;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ChannelHandler.Sharable
public class GoodGameCodec extends MessageToMessageCodec<JsonElement, GoodGameEvent> {
    private final Logger logger = LoggerFactory.getLogger(GoodGameCodec.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, JsonElement msg, List<Object> out) throws Exception {
        JsonObject rootObject = msg.getAsJsonObject();
        String type = rootObject.get("type").getAsString();
        JsonObject data = rootObject.getAsJsonObject("data");
        switch (type) {
            case "welcome": {
                String protocolVersion = data.get("protocolVersion").getAsString();
                out.add(new GoodGameEvent(GoodGameEventType.WELCOME, null, protocolVersion, null));
                break;
            }
            case "success_join": {
                String channel = data.get("channel_name").getAsString();
                out.add(new GoodGameEvent(GoodGameEventType.SUCCESS_JOIN, channel, null, null));
                break;
            }
            case "message": {
                String user = data.get("user_name").getAsString();
                String text = data.get("text").getAsString();
                String channel = data.get("channel_id").getAsString();
                out.add(new GoodGameEvent(GoodGameEventType.MESSAGE, channel, text, user));
                break;
            }
            case "user_ban": {
                String user = data.get("user_name").getAsString();
                out.add(new GoodGameEvent(GoodGameEventType.USER_BAN, null, null, user));
                break;
            }
            case "error": {
                String text = data.get("errorMsg").getAsString();
                out.add(new GoodGameEvent(GoodGameEventType.ERROR, null, text, null));
                break;
            }
            default: {
                logger.debug("unsupported message type {}", type);
            }
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, GoodGameEvent msg, List<Object> out) throws Exception {
        switch (msg.getType()) {
            case JOIN: {
                JsonObject rootObject = new JsonObject();
                rootObject.addProperty("type", "join");
                JsonObject dataObject = new JsonObject();
                dataObject.addProperty("channel_id", msg.getChannel());
                dataObject.addProperty("hidden", false);
                dataObject.addProperty("mobile", 0);
                rootObject.add("data", dataObject);
                out.add(rootObject);
                break;
            }
        }
    }
}
