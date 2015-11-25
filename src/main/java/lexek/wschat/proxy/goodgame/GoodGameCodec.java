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
        logger.debug(rootObject.toString());
        String type = rootObject.get("type").asText();
        JsonNode data = rootObject.get("data");
        switch (type) {
            case "welcome": {
                String protocolVersion = data.get("protocolVersion").asText();
                out.add(new GoodGameEvent(GoodGameEventType.WELCOME, null, protocolVersion, null, null));
                break;
            }
            case "success_auth": {
                out.add(new GoodGameEvent(
                    GoodGameEventType.SUCCESS_AUTH,
                    null,
                    null,
                    data.get("user_id").asText(),
                    null
                ));
                break;
            }
            case "success_join": {
                if (data.get("access_rights").asLong() >= 10 || data.get("user_id").asLong() == 0) {
                    String channel = data.get("channel_name").asText();
                    out.add(new GoodGameEvent(GoodGameEventType.SUCCESS_JOIN, channel, null, null, null));
                } else {
                    out.add(new GoodGameEvent(GoodGameEventType.BAD_RIGHTS, null, null, null, null));
                }
                break;
            }
            case "message": {
                String id = data.get("user_id").asText();
                String user = data.get("user_name").asText();
                String text = data.get("text").asText();
                String channel = data.get("channel_id").asText();
                out.add(new GoodGameEvent(GoodGameEventType.MESSAGE, channel, text, user, id));
                break;
            }
            case "user_ban": {
                String user = data.get("user_name").asText();
                out.add(new GoodGameEvent(GoodGameEventType.USER_BAN, null, null, user, null));
                break;
            }
            case "error": {
                String text = data.get("errorMsg").asText();
                if (data.get("error_num").asLong() == 0) {
                    out.add(new GoodGameEvent(GoodGameEventType.FAILED_JOIN, null, null, null, null));
                } else {
                    out.add(new GoodGameEvent(GoodGameEventType.ERROR, null, text, null, null));
                }
                break;
            }
            default: {
                logger.debug("unsupported message type {}: {}", type, data);
                break;
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
            case AUTH: {
                ObjectNode rootObject = JsonNodeFactory.instance.objectNode();
                rootObject.set("type", new TextNode("auth"));
                ObjectNode dataObject = JsonNodeFactory.instance.objectNode();
                dataObject.set("user_id", new TextNode(msg.getUser()));
                dataObject.set("token", new TextNode(msg.getText()));
                rootObject.set("data", dataObject);
                out.add(rootObject);
                break;
            }
            case BAN: {
                ObjectNode rootObject = JsonNodeFactory.instance.objectNode();
                rootObject.set("type", new TextNode("ban"));
                ObjectNode dataObject = JsonNodeFactory.instance.objectNode();
                dataObject.set("user_id", new TextNode(msg.getId()));
                dataObject.set("channel_id", new TextNode(msg.getChannel()));
                dataObject.set("ban_channel", new TextNode(msg.getChannel()));
                dataObject.set("duration", new LongNode(3600));
                dataObject.set("reason", new TextNode("proxy ban"));
                dataObject.set("comment", new TextNode("proxy ban"));
                dataObject.set("show_ban", BooleanNode.TRUE);
                rootObject.set("data", dataObject);
                out.add(rootObject);
                break;
            }
            default:
                logger.warn("Unsupported type {}", msg.getType());
                break;
        }
    }
}
