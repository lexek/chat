package lexek.wschat.chat;

import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.db.model.UserDto;

import java.util.List;
import java.util.Map;

public class MessageProperty<T> {
    private static Map<String, MessageProperty> KEYS = new ConcurrentHashMapV8<>();

    public static final MessageProperty<MessageType> TYPE = valueOf("type");
    public static final MessageProperty<String> ROOM = valueOf("room");
    public static final MessageProperty<String> NAME = valueOf("name");
    public static final MessageProperty<String> MOD = valueOf("mod");
    public static final MessageProperty<LocalRole> LOCAL_ROLE = valueOf("role");
    public static final MessageProperty<GlobalRole> GLOBAL_ROLE = valueOf("globalRole");
    public static final MessageProperty<String> COLOR = valueOf("color");
    public static final MessageProperty<Long> MESSAGE_ID = valueOf("messageId");
    public static final MessageProperty<Long> TIME = valueOf("time");
    public static final MessageProperty<String> TEXT = valueOf("text");
    public static final MessageProperty<UserDto> USER = valueOf("user");
    public static final MessageProperty<Chatter> CHATTER = valueOf("chatter");
    public static final MessageProperty<List<Message>> HISTORY_DATA = valueOf("history");
    public static final MessageProperty<List<Chatter>> NAMES = valueOf("names");
    public static final MessageProperty<List> ERROR_DATA = valueOf("errorData");
    public static final MessageProperty<List> SERVICE = valueOf("service");
    public static final MessageProperty<List> SERVICE_RESOURCE = valueOf("serviceResource");
    public static final MessageProperty<String> POLL_DATA = valueOf("pollData");
    public static final MessageProperty<Integer> POLL_OPTION = valueOf("pollOption");
    private final String name;

    private MessageProperty(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is null");
        }
        if (KEYS.containsKey(name)) {
            throw new IllegalArgumentException("Name is already taken");
        }
        this.name = name;
    }

    public static <T> MessageProperty<T> valueOf(String name) {
        return new MessageProperty<>(name);
    }

    public static boolean exists(String name) {
        return KEYS.containsKey(name);
    }

    public static <T> MessageProperty<T> get(String name) {
        return KEYS.get(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
