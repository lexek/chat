package lexek.wschat.proxy.goodgame;

import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class GoodGameProxyProvider extends ProxyProvider {
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messsageId;

    public GoodGameProxyProvider(EventLoopGroup eventLoopGroup, MessageBroadcaster messageBroadcaster, AtomicLong messsageId) {
        super("goodgame", false, false, EnumSet.of(ModerationOperation.BAN));
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messsageId = messsageId;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, String name, String key, boolean outbound) {
        return new GoodGameChatProxy(
            eventLoopGroup,
            remoteRoom,
            name,
            key,
            messageBroadcaster,
            messsageId,
            room,
            id,
            this
        );
    }

    @Override
    public boolean validateCredentials(String name, String token) {
        //todo: wait for better api on goodgame side
        return false;
    }
}
