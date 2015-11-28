package lexek.wschat.proxy;

import lexek.wschat.chat.model.Message;

public interface Proxy {
    void start();

    void stop();

    void moderate(ModerationOperation type, String name);

    void onMessage(Message message);

    long id();

    ProxyProvider provider();

    String remoteRoom();

    boolean outboundEnabled();

    boolean moderationEnabled();

    ProxyState state();

    String lastError();
}
