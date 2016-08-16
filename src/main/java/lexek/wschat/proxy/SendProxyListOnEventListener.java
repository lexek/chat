package lexek.wschat.proxy;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.ChatEventType;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.db.model.ProxyMessageModel;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.stream.Collectors;

@Service
public class SendProxyListOnEventListener implements EventListener {
    private final ProxyManager proxyManager;

    @Inject
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

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatEventType getEventType() {
        return ChatEventType.JOIN;
    }
}
