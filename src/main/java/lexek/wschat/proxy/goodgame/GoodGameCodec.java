package lexek.wschat.proxy.goodgame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ChannelHandler.Sharable
public class GoodGameCodec extends MessageToMessageCodec<JsonNode, GoodGameEvent> {
    private final Logger logger = LoggerFactory.getLogger(GoodGameCodec.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, JsonNode rootObject, List<Object> out) throws Exception {
        String type = rootObject.get("type").asText();
        JsonNode data = rootObject.get("data");
        switch (type) {
            case "welcome": {
                String protocolVersion = data.get("protocolVersion").asText();
                out.add(new GoodGameEvent(GoodGameEventType.WELCOME, null, protocolVersion, null));
                break;
            }
            case "success_join": {
                String channel = data.get("channel_name").asText();
                out.add(new GoodGameEvent(GoodGameEventType.SUCCESS_JOIN, channel, null, null));
                break;
            }
            case "message": {
                String user = data.get("user_name").asText();
                String text = data.get("text").asText();
                String channel = data.get("channel_id").asText();
                out.add(new GoodGameEvent(GoodGameEventType.MESSAGE, channel, text, user));
                break;
            }
            case "user_ban": {
                String user = data.get("user_name").asText();
                out.add(new GoodGameEvent(GoodGameEventType.USER_BAN, null, null, user));
                break;
            }
            case "error": {
                String text = data.get("errorMsg").asText();
                out.add(new GoodGameEvent(GoodGameEventType.ERROR, null, text, null));
                break;
            }
            default: {
                logger.debug("unsupported message type {}: {}", type, data);
            }
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, GoodGameEvent msg, List<Object> out) throws Exception {
        switch (msg.getType()) {
            case JOIN: {
                ObjectNode rootObject = JsonNodeFactory.instance.objectNode();
                rootObject.set("type", new TextNode("join"));
                ObjectNode dataObject = JsonNodeFactory.instance.objectNode();
                dataObject.set("channel_id", new TextNode(msg.getChannel()));
                dataObject.set("hidden", BooleanNode.valueOf(false));
                dataObject.set("mobile", new LongNode(0));
                rootObject.set("data", dataObject);
                out.add(rootObject);
                break;
            }
        }
    }
}
