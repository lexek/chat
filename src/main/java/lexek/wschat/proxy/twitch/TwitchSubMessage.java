package lexek.wschat.proxy.twitch;

import lexek.wschat.chat.msg.MessageNode;

import java.util.List;

public class TwitchSubMessage extends TwitchEventMessage {
    private final String userName;
    private final String userColor;
    private final int months;
    private final List<MessageNode> message;

    public TwitchSubMessage(String userName, String userColor, int months, List<MessageNode> message) {
        super(Type.SUB, null);
        this.userName = userName;
        this.userColor = userColor;
        this.months = months;
        this.message = message;
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

    public int getMonths() {
        return months;
    }

    @Override
    public String toString() {
        return "TwitchSubMessage{" +
            "userName='" + userName + '\'' +
            ", userColor='" + userColor + '\'' +
            ", months=" + months +
            ", message=" + message +
            "} " + super.toString();
    }
}
