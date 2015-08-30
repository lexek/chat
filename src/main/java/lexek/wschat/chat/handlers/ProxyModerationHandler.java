package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.ProxyManager;

public class ProxyModerationHandler extends AbstractRoomMessageHandler {
    private final ProxyManager proxyManager;

    public ProxyModerationHandler(ProxyManager proxyManager) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM,
                MessageProperty.NAME,
                MessageProperty.SERVICE
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
            proxyManager.moderate(room, message.get(MessageProperty.SERVICE), operation, message.get(MessageProperty.NAME));
        } catch (IllegalArgumentException e) {
            connection.send(Message.errorMessage("BAD_ARG"));
        }
    }
}
