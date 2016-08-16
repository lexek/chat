package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.ProxyManager;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class ProxyModerationHandler extends AbstractRoomMessageHandler {
    private final ProxyManager proxyManager;

    @Inject
    public ProxyModerationHandler(ProxyManager proxyManager) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM,
                MessageProperty.NAME,
                MessageProperty.SERVICE,
                MessageProperty.SERVICE_RESOURCE,
                MessageProperty.TEXT
            ),
            MessageType.PROXY_MOD,
            LocalRole.MOD,
            false);
        this.proxyManager = proxyManager;
    }

    @Override
    public void handle(Connection connection, User user, Room room, Chatter chatter, Message message) {
        try {
            ModerationOperation operation = ModerationOperation.valueOf(message.get(MessageProperty.TEXT));
            proxyManager.moderate(
                room,
                message.get(MessageProperty.SERVICE),
                message.get(MessageProperty.SERVICE_RESOURCE),
                operation, message.get(MessageProperty.NAME)
            );
        } catch (IllegalArgumentException e) {
            connection.send(Message.errorMessage("BAD_ARG"));
        }
    }
}
