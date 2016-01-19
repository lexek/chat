package lexek.wschat.proxy.sc2tv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lexek.netty.handler.codec.socketio.SocketIoEvent;
import lexek.netty.handler.codec.socketio.SocketIoPacketType;

import java.util.List;

public class Sc2tvCodec extends MessageToMessageCodec<SocketIoEvent<String>, Sc2tvMessage> {
    private final ObjectMapper objectMapper;

    public Sc2tvCodec() {
        this(new ObjectMapper());
    }

    public Sc2tvCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Sc2tvMessage message, List<Object> out) throws Exception {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(message.getScope());
        arrayNode.add(message.getData());
        out.add(new SocketIoEvent<>(
            SocketIoPacketType.EVENT,
            "/",
            message.getId(),
            objectMapper.writeValueAsString(arrayNode)
        ));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, SocketIoEvent<String> message, List<Object> out) throws Exception {
        JsonNode root = objectMapper.readTree(message.getData());
        if (message.getType() == SocketIoPacketType.EVENT) {
            out.add(new Sc2tvMessage(
                null,
                root.get(0).asText(),
                root.get(1)
            ));
        }
        if (message.getType() == SocketIoPacketType.ACK) {
            out.add(new Sc2tvAck(
                message.getId(),
                root.get(0)
            ));
        }
    }
}
