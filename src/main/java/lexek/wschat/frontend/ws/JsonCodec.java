package lexek.wschat.frontend.ws;

import com.google.common.collect.ImmutableSet;
import com.google.gson.*;
import lexek.wschat.chat.InboundMessage;
import lexek.wschat.chat.Message;
import lexek.wschat.chat.MessageType;
import lexek.wschat.chat.User;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.frontend.Codec;
import lexek.wschat.services.poll.PollState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class JsonCodec implements Codec {
    private static final Set<String> allowedUserFields = ImmutableSet.of(
        "name", "role", "color", "timedOut", "banned"
    );
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ThreadLocal<Gson> gson = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return (f.getDeclaringClass() == UserDto.class && !allowedUserFields.contains(f.getName())) ||
                        (f.getDeclaringClass() == PollState.class && f.getName().equals("voted"));
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
                .registerTypeAdapter(Message.class,
                    (JsonSerializer<Message>) (src, typeOfSrc, context) -> context.serialize(src.getData()))
                .create();
        }
    };

    @Override
    public String encode(Message message, User user) {
        return gson.get().toJson(message);
    }

    @Override
    public InboundMessage decode(String message) {
        InboundMessage result;
        try {
            result = gson.get().fromJson(message, InboundMessage.class);
        } catch (JsonSyntaxException e) {
            logger.warn("Deserialization error: {}", message);
            result = new InboundMessage(MessageType.UNKNOWN);
        }
        return result;
    }
}
