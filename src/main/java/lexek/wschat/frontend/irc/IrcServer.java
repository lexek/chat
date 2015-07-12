package lexek.wschat.frontend.irc;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableList;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import lexek.wschat.services.AbstractService;
import lexek.wschat.util.ExceptionLogger;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class IrcServer extends AbstractService {
    private static final int PORT = 6667;
    private final ServerBootstrap bootstrap;
    private Channel channel;

    public IrcServer(final IrcServerHandler handler, EventLoopGroup bossGroup, EventLoopGroup childGroup, final SslContext sslContext) {
        super("ircServer", ImmutableList.<String>of());

        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, childGroup);
        if (Epoll.isAvailable()) {
            this.bootstrap.channel(EpollServerSocketChannel.class);
        } else {
            this.bootstrap.channel(NioServerSocketChannel.class);
        }
        final ChannelHandler stringDecoder = new StringDecoder(StandardCharsets.UTF_8);
        final ChannelHandler stringEncoder = new StringEncoder(StandardCharsets.UTF_8);
        this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        this.bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final ExceptionLogger exceptionLogger = new ExceptionLogger();
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(sslContext.newHandler(channel.alloc()));
                pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                pipeline.addLast(stringDecoder);
                pipeline.addLast(new IdleStateHandler(120, 0, 140, TimeUnit.SECONDS));
                pipeline.addLast(stringEncoder);
                pipeline.addLast(handler);
                pipeline.addLast(exceptionLogger);
            }
        });
    }

    @Override
    public void performAction(String action) {
    }

    @Override
    public void stop() {
        if (this.channel.isOpen()) {
            this.channel.close();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public HealthCheck getHealthCheck() {
        return new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (channel.isActive()) {
                    return Result.healthy();
                } else {
                    return Result.unhealthy("Channel is inactive");
                }
            }
        };
    }

    @Override
    public void registerMetrics(MetricRegistry metricRegistry) {
        metricRegistry.register(getName() + ".port", (Gauge<Integer>) () -> PORT);
    }

    @Override
    protected void start0() {
        this.channel = bootstrap.bind(new InetSocketAddress(PORT)).awaitUninterruptibly().channel();
    }
}
