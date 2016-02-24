package lexek.wschat.proxy.sc2tv;

import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.services.NotificationService;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class Sc2tvProxyProvider extends ProxyProvider {
    private final NotificationService notificationService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;

    public Sc2tvProxyProvider(
        NotificationService notificationService, EventLoopGroup eventLoopGroup, MessageBroadcaster messageBroadcaster,
        AtomicLong messageId
    ) {
        super("sc2tv", false, false, EnumSet.noneOf(ModerationOperation.class));
        this.notificationService = notificationService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, String name, String key, boolean outbound) {
        return new Sc2tvChatProxy(
            notificationService, remoteRoom, messageBroadcaster, eventLoopGroup, messageId, room, id, this
        );
    }

    @Override
    public boolean validateCredentials(String name, String token) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        return true;
    }
}
