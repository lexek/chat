package lexek.wschat.frontend.ws;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import lexek.wschat.services.AbstractService;
import lexek.wschat.util.ExceptionLogger;

import java.util.concurrent.TimeUnit;

public class WebSocketChatServer extends AbstractService {
    private final ServerBootstrap bootstrap;
    private final int port;
    private Channel channel;

    public WebSocketChatServer(final int port, final WebSocketChatHandler handler, EventLoopGroup bossGroup,
                               EventLoopGroup childGroup, final SslContext sslContext) {
        super("websocketServer");
        this.port = port;
        final ChannelHandler flashPolicyHandler = new FlashPolicyFileHandler(port);
        final ExceptionLogger exceptionLogger = new ExceptionLogger();
        this.bootstrap = new ServerBootstrap();
        this.bootstrap.group(bossGroup, childGroup);

        if (Epoll.isAvailable()) {
            this.bootstrap.channel(EpollServerSocketChannel.class);
        } else {
            this.bootstrap.channel(NioServerSocketChannel.class);
        }
        this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        this.bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        this.bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("flash-policy", flashPolicyHandler);
                pipeline.addLast(sslContext.newHandler(ByteBufAllocator.DEFAULT));
                pipeline.addLast("http-codec", new HttpServerCodec(4096, 8192, 8192));
                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast(new WebSocketServerProtocolHandler("/", ""));
                pipeline.addLast(new IdleStateHandler(120, 0, 140, TimeUnit.SECONDS));
                pipeline.addLast(handler);
                pipeline.addLast(exceptionLogger);
            }
        });
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
        metricRegistry.register(getName() + ".port", (Gauge<Integer>) () -> port);
    }

    @Override
    protected void start0() {
        this.channel = bootstrap.bind(port).awaitUninterruptibly().channel();
    }
}
