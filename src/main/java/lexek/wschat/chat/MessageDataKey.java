package lexek.wschat.chat;

import io.netty.util.internal.chmv8.ConcurrentHashMapV8;

import java.util.Map;

public class MessageDataKey<T> {
    private static Map<String, MessageDataKey> KEYS = new ConcurrentHashMapV8<>();
    private final String name;

    private MessageDataKey(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is null");
        }
        if (KEYS.containsKey(name)) {
            throw new IllegalArgumentException("Name is already taken");
        }
        this.name = name;
    }

    public static <T> MessageDataKey<T> valueOf(String name) {
        return new MessageDataKey<>(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
