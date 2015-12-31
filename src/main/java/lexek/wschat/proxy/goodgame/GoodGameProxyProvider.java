package lexek.wschat.proxy.goodgame;

import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.services.NotificationService;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class GoodGameProxyProvider extends ProxyProvider {
    private final NotificationService notificationService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messsageId;

    public GoodGameProxyProvider(
        NotificationService notificationService, EventLoopGroup eventLoopGroup, MessageBroadcaster messageBroadcaster,
        AtomicLong messsageId
    ) {
        super("goodgame", false, false, EnumSet.noneOf(ModerationOperation.class));
        this.notificationService = notificationService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messsageId = messsageId;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, String name, String key, boolean outbound) {
        return new GoodGameChatProxy(
            notificationService, messageBroadcaster, eventLoopGroup, messsageId, this, id, room, remoteRoom, name, key
        );
    }

    @Override
    public boolean validateCredentials(String name, String token) {
        //todo: wait for better api on goodgame side
        return false;
    }
}
