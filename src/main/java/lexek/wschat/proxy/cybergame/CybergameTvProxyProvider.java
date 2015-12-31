package lexek.wschat.proxy.cybergame;

import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.services.NotificationService;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class CybergameTvProxyProvider extends ProxyProvider {
    private final NotificationService notificationService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;

    public CybergameTvProxyProvider(
        NotificationService notificationService, MessageBroadcaster messageBroadcaster, EventLoopGroup eventLoopGroup,
        AtomicLong messageId
    ) {
        super("cybergame", false, false, EnumSet.noneOf(ModerationOperation.class));
        this.notificationService = notificationService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, String name, String key, boolean outbound) {
        return new CybergameTvChatProxy(
            notificationService, messageBroadcaster, eventLoopGroup, messageId, this, room, remoteRoom, id
        );
    }

    @Override
    public boolean validateCredentials(String name, String token) {
        throw new UnsupportedOperationException();
    }
}
