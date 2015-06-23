package lexek.wschat.proxy.cybergame;

import com.google.gson.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Sharable
public class CybergameTvMessageCodec extends MessageToMessageCodec<TextWebSocketFrame, CybergameTvOutboundEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CybergameTvMessageCodec.class);
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    @Override
    protected void encode(ChannelHandlerContext ctx, CybergameTvOutboundEvent msg, List<Object> out) throws Exception {
        if (msg.getType() == CybergameTvEventType.AUTH) {
            logger.debug("encoding auth request for channel {}", msg.getChannel());
            JsonArray messages = new JsonArray();
            JsonObject authenticationMessage = new JsonObject();
            authenticationMessage.addProperty("command", "login");
            JsonObject message = new JsonObject();
            message.addProperty("login", "");
            message.addProperty("password", "");
            message.addProperty("channel", "#" + msg.getChannel());
            authenticationMessage.addProperty("message", gson.toJson(message));
            messages.add(new JsonPrimitive(gson.toJson(authenticationMessage)));
            out.add(new TextWebSocketFrame(gson.toJson(messages)));
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame msg, List<Object> out) throws Exception {
        String text = msg.text();
        logger.trace("raw message {}", text);
        if (text.equals("o")) {
            out.add(new CybergameTvInboundEvent(CybergameTvEventType.AUTH));
            logger.debug("decoded auth");
        } else if (text.startsWith("a")) {
            JsonArray messages = jsonParser.parse(text.substring(1)).getAsJsonArray();
            for (JsonElement element : messages) {
                JsonObject command = jsonParser.parse(element.getAsString()).getAsJsonObject();
                if (command.get("command").getAsString().equals("chatMessage")) {
                    JsonObject message = jsonParser.parse(command.get("message").getAsString()).getAsJsonObject();
                    out.add(new CybergameTvInboundMessage(
                            message.get("from").getAsString(),
                            message.get("text").getAsString())
                    );
                    logger.debug("decoded message");
                }
            }
        }
    }
}
