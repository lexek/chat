package lexek.wschat.proxy.twitch;

import lexek.wschat.chat.msg.MessageNode;

import java.util.List;

public class TwitchUserMessage extends TwitchEventMessage {
    private final String userName;
    private final String userColor;
    private final List<MessageNode> message;
    private final Long bits;

    public TwitchUserMessage(String userName, String userColor, List<MessageNode> message, Long bits) {
        super(Type.MSG, null);
        this.userName = userName;
        this.userColor = userColor;
        this.message = message;
        this.bits = bits;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserColor() {
        return userColor;
    }

    public List<MessageNode> getMessage() {
        return message;
    }

    public Long getBits() {
        return bits;
    }

    @Override
    public String toString() {
        return "TwitchUserMessage{" +
            "userName='" + userName + '\'' +
            ", userColor='" + userColor + '\'' +
            ", message=" + message +
            ", bits=" + bits +
            "} " + super.toString();
    }
}
