package lexek.wschat.proxy.twitch;

import lexek.wschat.chat.msg.MessageNode;

import java.util.List;

public interface JTVEventListener {
    void onDisconnected();

    void onMessage(String userName, String color, List<MessageNode> message);

    void onSub(String userName, String color, List<MessageNode> message, int months);

    void onClear(String name);

    void loginFailed();

    void selfJoined(String room);

    void exceptionCaught(Throwable throwable);
}
