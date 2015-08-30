package lexek.wschat.proxy.cybergame;

import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class CybergameTvProxyProvider extends ProxyProvider {
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;

    public CybergameTvProxyProvider(EventLoopGroup eventLoopGroup,
                                    MessageBroadcaster messageBroadcaster,
                                    AtomicLong messageId) {
        super("cybergame", false, false, EnumSet.noneOf(ModerationOperation.class));
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, String name, String key, boolean outbound) {
        return new CybergameTvChatProxy(
            eventLoopGroup,
            remoteRoom,
            this,
            messageBroadcaster,
            messageId,
            room,
            id
        );
    }

    @Override
    public boolean validateCredentials(String name, String token) {
        throw new UnsupportedOperationException();
    }
}
