package lexek.wschat.proxy.twitch;

import lexek.wschat.chat.msg.MessageNode;

import java.util.List;

public interface JTVEventListener {
    void onDisconnected();

    void onMessage(String userName, List<MessageNode> message);

    void onClear(String name);

    void loginFailed();

    void selfJoined(String room);

    void exceptionCaught(Throwable throwable);
}
