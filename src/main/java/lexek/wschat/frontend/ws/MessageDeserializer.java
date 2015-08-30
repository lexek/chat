package lexek.wschat.frontend.ws;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.LocalRole;
import lexek.wschat.chat.Message;
import lexek.wschat.chat.MessageProperty;
import lexek.wschat.chat.MessageType;

import java.io.IOException;

public class MessageDeserializer extends StdDeserializer<Message> {
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
                    mapBuilder.put(MessageProperty.NAME, node.asText());
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
            }

        });
        return new Message(mapBuilder.build());
    }
}
