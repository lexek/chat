package lexek.wschat.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lexek.wschat.services.NotificationService;

public abstract class AbstractNettyProxy extends AbstractProxy {
    protected final EventLoopGroup eventLoopGroup;
    protected final Bootstrap bootstrap;
    protected volatile Channel channel;

    protected AbstractNettyProxy(EventLoopGroup eventLoopGroup, NotificationService notificationService, ProxyDescriptor descriptor) {
        super(eventLoopGroup, notificationService, descriptor);
        this.eventLoopGroup = eventLoopGroup;
        this.bootstrap = new Bootstrap();

        bootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    @Override
    protected void disconnect() {
        if (this.channel != null && this.channel.isActive()) {
            this.channel.close();
        }
    }
}
