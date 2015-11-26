package lexek.wschat.frontend.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.chat.model.User;
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
    public Message decode(String message) {
        Message result;
        try {
            result = objectMapper.readValue(message, Message.class);
        } catch (IOException e) {
            logger.warn("Deserialization error: {}", e);
            result = new Message(ImmutableMap.of(MessageProperty.TYPE, MessageType.UNKNOWN));
        }
        return result;
    }
}
