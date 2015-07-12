package lexek.wschat.chat;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.db.model.Chatter;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.poll.PollState;

import java.util.List;

public class Message {
    private final ImmutableMap<MessageDataKey, Object> data;

    public Message(ImmutableMap<MessageDataKey, Object> data) {
        if (!data.containsKey(Keys.TYPE)) {
            throw new IllegalArgumentException("Data must contain type.");
        }
        this.data = data;
    }

    public static Message errorMessage(String text) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.ERROR, Keys.TEXT, text));
    }

    public static Message authCompleteMessage(UserDto user) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.AUTH_COMPLETE, Keys.USER, user));
    }

    public static Message errorMessage(String text, List errorData) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.ERROR, Keys.TEXT, text, Keys.ERROR_DATA, errorData));
    }

    public static Message infoMessage(String text) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.INFO, Keys.TEXT, text));
    }

    public static Message infoMessage(String text, String room) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.INFO, Keys.TEXT, text, Keys.ROOM, room));
    }

    public static Message joinMessage(String room, UserDto user) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.JOIN, Keys.ROOM, room, Keys.USER, user));
    }

    public static Message partMessage(String room, String name) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.PART, Keys.ROOM, room, Keys.NAME, name));
    }

    public static Message selfJoinMessage(String room, Chatter chatter) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.SELF_JOIN,
            Keys.ROOM, room, Keys.CHATTER, chatter));
    }

    public static Message historyMessage(List<Message> messages) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.HIST, Keys.HISTORY_DATA, messages));
    }

    public static Message pollMessage(MessageType type, String room, PollState pollState) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, type, Keys.ROOM, room, Keys.POLL_DATA, pollState));
    }

    public static Message pollVotedMessage(String room) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.POLL_VOTED, Keys.ROOM, room));
    }

    public static Message namesMessage(String room, List<Chatter> users) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.NAMES, Keys.ROOM, room, Keys.NAMES, users));
    }

    public static Message moderationMessage(MessageType messageType, String room, String mod, String user) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, messageType, Keys.ROOM, room, Keys.NAME, user, Keys.MOD, mod));
    }

    public static Message clearMessage(String room) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.CLEAR_ROOM, Keys.ROOM, room));
    }

    public static Message colorMessage(String color) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.COLOR, Keys.COLOR, color));
    }

    public static Message pongMessage(String text) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.PONG, Keys.TEXT, text));
    }

    public static Message captchaMessage(String captchaId) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, MessageType.RECAPTCHA, Keys.TEXT, captchaId));
    }

    public static Message msgMessage(String room, String name, LocalRole role, GlobalRole globalRole, String color, long messageId, long time, String text) {
        return new Message(ImmutableMap.<MessageDataKey, Object>builder()
            .put(Keys.TYPE, MessageType.MSG)
            .put(Keys.ROOM, room)
            .put(Keys.NAME, name)
            .put(Keys.GLOBAL_ROLE, globalRole)
            .put(Keys.LOCAL_ROLE, role)
            .put(Keys.COLOR, color)
            .put(Keys.MESSAGE_ID, messageId)
            .put(Keys.TIME, time)
            .put(Keys.TEXT, text)
            .build());
    }

    public static Message extMessage(String room, String name, LocalRole role, GlobalRole globalRole, String color, long messageId, long time,
                                     String text, String service, String serviceResource) {
        return new Message(ImmutableMap.<MessageDataKey, Object>builder()
            .put(Keys.TYPE, MessageType.MSG_EXT)
            .put(Keys.ROOM, room)
            .put(Keys.NAME, name)
            .put(Keys.GLOBAL_ROLE, globalRole)
            .put(Keys.LOCAL_ROLE, role)
            .put(Keys.COLOR, color)
            .put(Keys.MESSAGE_ID, messageId)
            .put(Keys.TIME, time)
            .put(Keys.TEXT, text)
            .put(Keys.SERVICE, service)
            .put(Keys.SERVICE_RESOURCE, serviceResource)
            .build());
    }

    public static Message meMessage(String room, String name, LocalRole role, GlobalRole globalRole, String color, long messageId, long time, String text) {
        return new Message(ImmutableMap.<MessageDataKey, Object>builder()
            .put(Keys.TYPE, MessageType.ME)
            .put(Keys.ROOM, room)
            .put(Keys.NAME, name)
            .put(Keys.GLOBAL_ROLE, globalRole)
            .put(Keys.LOCAL_ROLE, role)
            .put(Keys.COLOR, color)
            .put(Keys.MESSAGE_ID, messageId)
            .put(Keys.TIME, time)
            .put(Keys.TEXT, text)
            .build());
    }

    public static Message likeMessage(String room, String name, long messageId) {
        return new Message(ImmutableMap.<MessageDataKey, Object>builder()
            .put(Keys.TYPE, MessageType.LIKE)
            .put(Keys.ROOM, room)
            .put(Keys.NAME, name)
            .put(Keys.MESSAGE_ID, messageId)
            .build());
    }

    public static Message emptyMessage(MessageType messageType) {
        return new Message(ImmutableMap.<MessageDataKey, Object>of(Keys.TYPE, messageType));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(MessageDataKey<T> key) {
        return (T) data.get(key);
    }

    public MessageType getType() {
        return (MessageType) data.get(Keys.TYPE);
    }

    public ImmutableMap<MessageDataKey, Object> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        return data.equals(message.data);

    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public static class Keys {
        public static final MessageDataKey<MessageType> TYPE = MessageDataKey.valueOf("type");
        public static final MessageDataKey<String> ROOM = MessageDataKey.valueOf("room");
        public static final MessageDataKey<String> NAME = MessageDataKey.valueOf("name");
        public static final MessageDataKey<String> MOD = MessageDataKey.valueOf("mod");
        public static final MessageDataKey<LocalRole> LOCAL_ROLE = MessageDataKey.valueOf("role");
        public static final MessageDataKey<GlobalRole> GLOBAL_ROLE = MessageDataKey.valueOf("globalRole");
        public static final MessageDataKey<String> COLOR = MessageDataKey.valueOf("color");
        public static final MessageDataKey<Long> MESSAGE_ID = MessageDataKey.valueOf("messageId");
        public static final MessageDataKey<Long> TIME = MessageDataKey.valueOf("time");
        public static final MessageDataKey<String> TEXT = MessageDataKey.valueOf("text");
        public static final MessageDataKey<UserDto> USER = MessageDataKey.valueOf("user");
        public static final MessageDataKey<Chatter> CHATTER = MessageDataKey.valueOf("chatter");
        public static final MessageDataKey<List<Message>> HISTORY_DATA = MessageDataKey.valueOf("history");
        public static final MessageDataKey<List<Chatter>> NAMES = MessageDataKey.valueOf("names");
        public static final MessageDataKey<List> ERROR_DATA = MessageDataKey.valueOf("errorData");
        public static final MessageDataKey<List> SERVICE = MessageDataKey.valueOf("service");
        public static final MessageDataKey<List> SERVICE_RESOURCE = MessageDataKey.valueOf("serviceResource");
        public static final MessageDataKey<String> POLL_DATA = MessageDataKey.valueOf("pollData");
    }
}
