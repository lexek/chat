package lexek.wschat.proxy.twitch;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableList;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.chat.*;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.AbstractService;
import lexek.wschat.util.Colors;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class TwitchTvChatProxy extends AbstractService {
    private final String channel;
    private final ConnectionManager connectionManager;
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private final Room room;
    private final Bootstrap inboundBootstrap;
    private final Map<String, Channel> connections = new ConcurrentHashMapV8<>();
    private OutboundMessageHandler outboundHandler;
    private Channel activeInboundChannel = null;

    public TwitchTvChatProxy(final String channel,
                             ConnectionManager connectionManager,
                             AtomicLong messageId,
                             MessageBroadcaster messageBroadcaster,
                             AuthenticationManager authenticationManager,
                             Room room,
                             EventLoopGroup eventLoopGroup) {
        super("twitch.tv", ImmutableList.<String>of());
        this.channel = channel;
        this.connectionManager = connectionManager;
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.room = room;
        this.outboundHandler = new OutboundMessageHandler(connections, channel, authenticationManager, eventLoopGroup, room);

        this.inboundBootstrap = new Bootstrap();
        this.inboundBootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            this.inboundBootstrap.channel(EpollSocketChannel.class);
        } else {
            this.inboundBootstrap.channel(NioSocketChannel.class);
        }
        this.inboundBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        this.inboundBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final ChannelHandler stringEncoder = new StringEncoder(CharsetUtil.UTF_8);
        final ChannelHandler stringDecoder = new StringDecoder(CharsetUtil.UTF_8);
        final JTVEventListener eventListener = new JtvEventListenerImpl();
        this.inboundBootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                pipeline.addLast(stringEncoder);
                pipeline.addLast(stringDecoder);
                pipeline.addLast(new TwitchTvMessageDecoder());
                pipeline.addLast(new TwitchMessageHandler(eventListener, channel));
            }
        });
    }

    @Override
    public void start0() {
        connectInbound();
        connectionManager.registerService(outboundHandler);
    }

    @Override
    public void registerMetrics(MetricRegistry metricRegistry) {
        metricRegistry.register(getName() + ".activeOutboundConnections",
            (Gauge<Integer>) outboundHandler::getConnectionCount);
    }

    @Override
    public HealthCheck getHealthCheck() {
        return new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (activeInboundChannel == null || !activeInboundChannel.isActive()) {
                    return Result.unhealthy("inbound channel issue");
                }
                return Result.healthy();
            }
        };
    }

    @Override
    public void performAction(String action) {
    }

    @Override
    public void stop() {
    }

    private void connectInbound() {
        this.activeInboundChannel = inboundBootstrap.connect("irc.twitch.tv", 6667).channel();
    }

    private class JtvEventListenerImpl implements JTVEventListener {
        @Override
        public void onConnected() {
            logger.info("Twitch proxy connected. Channel: {}", channel);
        }

        @Override
        public void onDisconnected() {
            logger.info("Twitch proxy disconnected.");
            connectInbound();
        }

        @Override
        public void onMessage(TwitchUser user, String message) {
            if (!connections.containsKey(user.getNick().toLowerCase())) {
                Message msg = Message.extMessage(
                    room.getName(),
                    user.getNick(),
                    LocalRole.USER,
                    GlobalRole.USER,
                    Colors.generateColor(user.getNick()),
                    messageId.getAndIncrement(),
                    System.currentTimeMillis(),
                    message,
                    "twitch.tv",
                    channel
                );
                messageBroadcaster.submitMessage(msg, Connection.STUB_CONNECTION, room.FILTER);
            }
        }

        @Override
        public void onClear(String name) {
            Message msg = Message.moderationMessage(
                MessageType.CLEAR_EXT,
                "#main",
                "*twitch_ext",
                name
            );
            messageBroadcaster.submitMessage(msg, Connection.STUB_CONNECTION, room.FILTER);
        }

        @Override
        public void onServerMessage(String s) {
            logger.trace(s);
        }
    }
}
