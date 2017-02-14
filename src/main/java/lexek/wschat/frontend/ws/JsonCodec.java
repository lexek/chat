package lexek.wschat.frontend.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.frontend.http.rest.view.MessageView;
import lexek.wschat.util.BufferInputStream;
import lexek.wschat.util.BufferOutputStream;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Service
public class JsonCodec {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new MessageModule());
    private final ObjectWriter objectWriter = objectMapper.writerWithView(MessageView.class);

    public TextWebSocketFrame encode(Message message) {
        try {
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
            objectWriter.writeValue(new BufferOutputStream(buffer), message);
            return new TextWebSocketFrame(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Message decode(TextWebSocketFrame message) {
        Message result;
        try {
            result = objectMapper.readValue(new BufferInputStream(message.content()), Message.class);
        } catch (IOException e) {
            logger.warn("Deserialization error: {}", e);
            result = new Message(ImmutableMap.of(MessageProperty.TYPE, MessageType.UNKNOWN));
        }
        return result;
    }
}
