package lexek.wschat.proxy.cybergame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Sharable
public class CybergameTvMessageCodec extends MessageToMessageCodec<TextWebSocketFrame, CybergameTvEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CybergameTvMessageCodec.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, CybergameTvEvent msg, List<Object> out) throws Exception {
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("type", msg.getType());
        rootNode.set("data", msg.getData());
        out.add(new TextWebSocketFrame(objectMapper.writeValueAsString(rootNode)));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame msg, List<Object> out) throws Exception {
        String text = msg.text();
        logger.trace("raw message {}", text);
        JsonNode rootNode = objectMapper.readTree(text);
        out.add(new CybergameTvEvent(rootNode.get("type").asText(), rootNode.get("data")));
    }
}
