package lexek.wschat.proxy.goodgame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ChannelHandler.Sharable
public class GsonCodec extends MessageToMessageCodec<TextWebSocketFrame, JsonNode> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    Logger logger = LoggerFactory.getLogger(GsonCodec.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, JsonNode msg, List<Object> out) throws Exception {
        out.add(new TextWebSocketFrame(objectMapper.writeValueAsString(msg)));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame msg, List<Object> out) throws Exception {
        logger.trace("raw message: {}", msg);
        out.add(objectMapper.readTree(msg.text()));
    }
}
