package lexek.wschat.proxy;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.db.model.ProxyMessageModel;

import java.util.stream.Collectors;

public class SendProxyListOnEventListener implements EventListener {
    private final ProxyManager proxyManager;

    public SendProxyListOnEventListener(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        connection.send(Message.proxyListMessage(
            proxyManager.getProxiesByRoom(room)
                .stream()
                .map(proxy -> new ProxyMessageModel(
                    proxy.provider().getName(),
                    proxy.remoteRoom(),
                    proxy.outboundEnabled(),
                    proxy.moderationEnabled()
                ))
                .collect(Collectors.toList())
            ,
            room.getName()
        ));
    }
}
