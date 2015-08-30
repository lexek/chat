package lexek.wschat.proxy;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Message;

public interface Proxy {
    void start();

    void stop();

    void moderate(ModerationOperation type, String name);

    void onMessage(Connection connection, Message message);

    long id();

    ProxyProvider provider();

    String remoteRoom();

    boolean outboundEnabled();

    ProxyState state();

    String lastError();
}
