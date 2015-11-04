package lexek.wschat.chat;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.db.model.ProxyMessageModel;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.services.poll.PollState;

import java.util.List;

public class Message {
    private final ImmutableMap<MessageProperty, Object> data;

    public Message(ImmutableMap<MessageProperty, Object> data) {
        if (!data.containsKey(MessageProperty.TYPE)) {
            throw new IllegalArgumentException("Data must contain type.");
        }
        this.data = data;
    }

    public static Message ignoreMessage(MessageType type, String name) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, type, MessageProperty.NAME, name));
    }

    public static Message errorMessage(String text) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.ERROR, MessageProperty.TEXT, text));
    }

    public static Message authCompleteMessage(UserDto user) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.AUTH_COMPLETE, MessageProperty.USER, user));
    }

    public static Message errorMessage(String text, List errorData) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.ERROR, MessageProperty.TEXT, text, MessageProperty.ERROR_DATA, errorData));
    }

    public static Message infoMessage(String text) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.INFO, MessageProperty.TEXT, text));
    }

    public static Message infoMessage(String text, String room) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.INFO, MessageProperty.TEXT, text, MessageProperty.ROOM, room));
    }

    public static Message joinMessage(String room, UserDto user) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.JOIN, MessageProperty.ROOM, room, MessageProperty.USER, user));
    }

    public static Message partMessage(String room, String name) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.PART, MessageProperty.ROOM, room, MessageProperty.NAME, name));
    }

    public static Message selfJoinMessage(String room, Chatter chatter) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.SELF_JOIN,
            MessageProperty.ROOM, room, MessageProperty.CHATTER, chatter));
    }

    public static Message historyMessage(List<Message> messages) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.HIST, MessageProperty.HISTORY_DATA, messages));
    }

    public static Message pollMessage(MessageType type, String room, PollState pollState) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, type, MessageProperty.ROOM, room, MessageProperty.POLL_DATA, pollState));
    }

    public static Message pollVotedMessage(String room) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.POLL_VOTED, MessageProperty.ROOM, room));
    }

    public static Message namesMessage(String room, List<Chatter> users) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.NAMES, MessageProperty.ROOM, room, MessageProperty.CHATTERS, users));
    }

    public static Message moderationMessage(MessageType messageType, String room, String mod, String user) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, messageType, MessageProperty.ROOM, room, MessageProperty.NAME, user, MessageProperty.MOD, mod));
    }

    public static Message proxyClear(String room, String service, String remoteRoom, String user) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(
            MessageProperty.TYPE, MessageType.PROXY_CLEAR,
            MessageProperty.SERVICE, service,
            MessageProperty.SERVICE_RESOURCE, remoteRoom,
            MessageProperty.ROOM, room,
            MessageProperty.NAME, user
        ));
    }

    public static Message clearMessage(String room) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.CLEAR_ROOM, MessageProperty.ROOM, room));
    }

    public static Message colorMessage(String color) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.COLOR, MessageProperty.COLOR, color));
    }

    public static Message pongMessage(String text) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.PONG, MessageProperty.TEXT, text));
    }

    public static Message captchaMessage(String captchaId) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, MessageType.RECAPTCHA, MessageProperty.TEXT, captchaId));
    }

    public static Message msgMessage(String room, String name, LocalRole role, GlobalRole globalRole, String color, long messageId, long time, String text) {
        return new Message(ImmutableMap.<MessageProperty, Object>builder()
            .put(MessageProperty.TYPE, MessageType.MSG)
            .put(MessageProperty.ROOM, room)
            .put(MessageProperty.NAME, name)
            .put(MessageProperty.GLOBAL_ROLE, globalRole)
            .put(MessageProperty.LOCAL_ROLE, role)
            .put(MessageProperty.COLOR, color)
            .put(MessageProperty.MESSAGE_ID, messageId)
            .put(MessageProperty.TIME, time)
            .put(MessageProperty.TEXT, text)
            .build());
    }

    public static Message extMessage(String room, String name, LocalRole role, GlobalRole globalRole, String color, long messageId, long time,
                                     String text, String service, String serviceResource) {
        return new Message(ImmutableMap.<MessageProperty, Object>builder()
            .put(MessageProperty.TYPE, MessageType.MSG_EXT)
            .put(MessageProperty.ROOM, room)
            .put(MessageProperty.NAME, name)
            .put(MessageProperty.GLOBAL_ROLE, globalRole)
            .put(MessageProperty.LOCAL_ROLE, role)
            .put(MessageProperty.COLOR, color)
            .put(MessageProperty.MESSAGE_ID, messageId)
            .put(MessageProperty.TIME, time)
            .put(MessageProperty.TEXT, text)
            .put(MessageProperty.SERVICE, service)
            .put(MessageProperty.SERVICE_RESOURCE, serviceResource)
            .build());
    }

    public static Message meMessage(String room, String name, LocalRole role, GlobalRole globalRole, String color, long messageId, long time, String text) {
        return new Message(ImmutableMap.<MessageProperty, Object>builder()
            .put(MessageProperty.TYPE, MessageType.ME)
            .put(MessageProperty.ROOM, room)
            .put(MessageProperty.NAME, name)
            .put(MessageProperty.GLOBAL_ROLE, globalRole)
            .put(MessageProperty.LOCAL_ROLE, role)
            .put(MessageProperty.COLOR, color)
            .put(MessageProperty.MESSAGE_ID, messageId)
            .put(MessageProperty.TIME, time)
            .put(MessageProperty.TEXT, text)
            .build());
    }

    public static Message likeMessage(String room, String name, long messageId) {
        return new Message(ImmutableMap.<MessageProperty, Object>builder()
            .put(MessageProperty.TYPE, MessageType.LIKE)
            .put(MessageProperty.ROOM, room)
            .put(MessageProperty.NAME, name)
            .put(MessageProperty.MESSAGE_ID, messageId)
            .build());
    }

    public static Message emptyMessage(MessageType messageType) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(MessageProperty.TYPE, messageType));
    }

    public static Message proxyListMessage(List<ProxyMessageModel> proxies, String room) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(
            MessageProperty.TYPE, MessageType.PROXIES,
            MessageProperty.ROOM, room,
            MessageProperty.PROXIES, proxies
        ));
    }

    public static Message protocolMessage(long version) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(
            MessageProperty.TYPE, MessageType.PROTOCOL_VERSION,
            MessageProperty.VERSION, version
        ));
    }

    public static Message ignoredMessage(List<String> ignoredNames) {
        return new Message(ImmutableMap.<MessageProperty, Object>of(
            MessageProperty.TYPE, MessageType.IGNORED,
            MessageProperty.NAMES, ignoredNames
        ));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(MessageProperty<T> key) {
        return (T) data.get(key);
    }

    public ImmutableMap<MessageProperty, Object> getData() {
        return data;
    }

    public MessageType getType() {
        return (MessageType) data.get(MessageProperty.TYPE);
    }

    public String getText() {
        return get(MessageProperty.TEXT);
    }

    public String getRoom() {
        return get(MessageProperty.ROOM);
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
}
