package lexek.wschat.frontend.ws;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.model.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MessageDeserializer extends StdDeserializer<Message> {
    private final Logger logger = LoggerFactory.getLogger(MessageDeserializer.class);

    public MessageDeserializer() {
        super(Message.class);
    }

    @Override
    public Message deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
        ObjectNode rootNode = jp.readValueAsTree();
        ImmutableMap.Builder<MessageProperty, Object> mapBuilder = ImmutableMap.builder();
        rootNode.fields().forEachRemaining(entry -> {
            String name = entry.getKey();
            JsonNode node = entry.getValue();
            switch (name) {
                case "type":
                    try {
                        mapBuilder.put(MessageProperty.TYPE, MessageType.valueOf(node.asText()));
                    } catch (IllegalArgumentException e) {
                        mapBuilder.put(MessageProperty.TYPE, MessageType.UNKNOWN);
                    }
                    break;
                case "role":
                    mapBuilder.put(MessageProperty.LOCAL_ROLE, LocalRole.valueOf(node.asText()));
                    break;
                case "room":
                    mapBuilder.put(MessageProperty.ROOM, node.asText());
                    break;
                case "name":
                    String value = node.asText();
                    if (value != null) {
                        value = value.trim().toLowerCase();
                        mapBuilder.put(MessageProperty.NAME, value);
                    }
                    break;
                case "color":
                    mapBuilder.put(MessageProperty.COLOR, node.asText());
                    break;
                case "text":
                    mapBuilder.put(MessageProperty.TEXT, node.asText());
                    break;
                case "messageId":
                    mapBuilder.put(MessageProperty.MESSAGE_ID, node.asLong());
                    break;
                case "pollOption":
                    mapBuilder.put(MessageProperty.POLL_OPTION, node.asInt());
                    break;
                case "service":
                    mapBuilder.put(MessageProperty.SERVICE, node.asText());
                    break;
                case "serviceResource":
                    mapBuilder.put(MessageProperty.SERVICE_RESOURCE, node.asText());
                    break;
                default:
                    logger.warn("unsupported property {}", name);
                    break;
            }

        });
        return new Message(mapBuilder.build());
    }
}
