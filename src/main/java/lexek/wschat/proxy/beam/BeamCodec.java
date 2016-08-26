package lexek.wschat.proxy.beam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class BeamCodec extends MessageToMessageCodec<TextWebSocketFrame, JsonNode> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, JsonNode node, List<Object> out) throws Exception {
        out.add(new TextWebSocketFrame(objectMapper.writeValueAsString(node)));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame frame, List<Object> out) throws Exception {
        out.add(objectMapper.readTree(frame.text()));
    }
}
