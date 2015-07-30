package lexek.wschat.frontend.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lexek.wschat.chat.InboundMessage;
import lexek.wschat.chat.Message;
import lexek.wschat.chat.MessageType;
import lexek.wschat.chat.User;
import lexek.wschat.frontend.Codec;
import lexek.wschat.frontend.http.rest.view.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JsonCodec implements Codec {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new MessageModule());

    @Override
    public String encode(Message message, User user) {
        try {
            return objectMapper.writerWithView(MessageView.class).writeValueAsString(message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public InboundMessage decode(String message) {
        InboundMessage result;
        try {
            result = objectMapper.readValue(message, InboundMessage.class);
        } catch (IOException e) {
            logger.warn("Deserialization error: {}", e);
            result = new InboundMessage(MessageType.UNKNOWN);
        }
        return result;
    }
}
