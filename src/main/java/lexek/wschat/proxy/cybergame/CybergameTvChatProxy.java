package lexek.wschat.proxy.cybergame;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableList;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateHandler;
import lexek.wschat.chat.*;
import lexek.wschat.services.AbstractService;
import lexek.wschat.util.Colors;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CybergameTvChatProxy extends AbstractService {
    private final String channel;
    private final MessageBroadcaster messageBroadcaster;
    private final EventLoopGroup eventLoopGroup;
    private final AtomicLong messageId;
    private final Room room;
    private Bootstrap bootstrap;

    public CybergameTvChatProxy(EventLoopGroup eventLoopGroup, String channel,
                                MessageBroadcaster messageBroadcaster,
                                AtomicLong messageId, Room room) {
        super("cybergame.tv", ImmutableList.<String>of());
        this.eventLoopGroup = eventLoopGroup;
        this.channel = channel;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.room = room;
    }


    @Override
    protected void start0() {
        URI uri_ = null;
        try {
            uri_ = new URI("ws://cybergame.tv:9090/123/agerh4tt/websocket");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        final URI uri = uri_;
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            this.bootstrap.channel(EpollSocketChannel.class);
        } else {
            this.bootstrap.channel(NioSocketChannel.class);
        }
        this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final ChannelHandler handler = new CybergameTvChannelHandler();
        final CybergameTvMessageCodec codec = new CybergameTvMessageCodec();
        final CybergameTvProtocolHandler protocolHandler = new CybergameTvProtocolHandler(channel);
        this.bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel c) throws Exception {
                ChannelPipeline pipeline = c.pipeline();
                pipeline.addLast("http-codec", new HttpClientCodec(4096, 8192, 8192));
                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast(new IdleStateHandler(120, 0, 140, TimeUnit.SECONDS));
                pipeline.addLast(new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, null, false, null, 4096));
                pipeline.addLast(codec);
                pipeline.addLast(protocolHandler);
                pipeline.addLast(handler);
            }
        });
        this.bootstrap.connect("cybergame.tv", 9090);
    }

    @Override
    public void performAction(String action) {
    }

    @Override
    public void stop() {
    }

    @Override
    public HealthCheck getHealthCheck() {
        return new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy();
            }
        };
    }

    @Sharable
    private class CybergameTvChannelHandler extends SimpleChannelInboundHandler<CybergameTvInboundMessage> {
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            bootstrap.connect("cybergame.tv", 9090);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, CybergameTvInboundMessage message) throws Exception {
            Message chatMessage = Message.extMessage(
                "#main",
                message.getFrom(),
                LocalRole.USER,
                GlobalRole.USER,
                Colors.generateColor(message.getFrom()),
                messageId.getAndIncrement(),
                System.currentTimeMillis(),
                message.getText(),
                "cybergame.tv",
                channel
            );
            messageBroadcaster.submitMessage(
                chatMessage,
                Connection.STUB_CONNECTION,
                room.FILTER);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt == IdleState.READER_IDLE) {
                ctx.writeAndFlush(new PingWebSocketFrame());
            } else if (evt == IdleState.WRITER_IDLE) {
                ctx.close();
            }
        }
    }
}
