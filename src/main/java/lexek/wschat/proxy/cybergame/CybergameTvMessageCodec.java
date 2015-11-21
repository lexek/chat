package lexek.wschat.proxy.cybergame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, CybergameTvOutboundEvent msg, List<Object> out) throws Exception {
        if (msg.getType() == CybergameTvEventType.AUTH) {
            logger.debug("encoding auth request for channel {}", msg.getChannel());
            ArrayNode messages = JsonNodeFactory.instance.arrayNode();
            ObjectNode authenticationMessage = JsonNodeFactory.instance.objectNode();
            authenticationMessage.set("command", new TextNode("login"));
            ObjectNode message = JsonNodeFactory.instance.objectNode();
            message.set("login", new TextNode(""));
            message.set("password", new TextNode(""));
            message.set("channel", new TextNode("#" + msg.getChannel()));
            authenticationMessage.set("message", new TextNode(objectMapper.writeValueAsString(message)));
            messages.add(new TextNode(objectMapper.writeValueAsString(authenticationMessage)));
            out.add(new TextWebSocketFrame(objectMapper.writeValueAsString(messages)));
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
            JsonNode messages = objectMapper.readTree(text.substring(1));
            for (JsonNode element : messages) {
                JsonNode command = objectMapper.readTree(element.asText());
                if (command.get("command").asText().equals("chatMessage")) {
                    JsonNode message = objectMapper.readTree(command.get("message").asText());
                    out.add(new CybergameTvInboundMessage(
                        message.get("from").asText(),
                        message.get("text").asText())
                    );
                    logger.debug("decoded message");
                }
            }
        }
    }
}
