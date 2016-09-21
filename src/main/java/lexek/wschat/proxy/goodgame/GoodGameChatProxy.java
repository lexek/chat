package lexek.wschat.proxy.goodgame;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
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
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.proxy.*;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.Colors;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class GoodGameChatProxy extends AbstractProxy {
    private static final String HOST_NAME = "chat.goodgame.ru";
    private final Cache<String, String> idCache = CacheBuilder.newBuilder().maximumSize(100).build();
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final Room room;
    private final Bootstrap bootstrap;
    private final String userId;
    private volatile Channel channel;
    private String channelName;

    public GoodGameChatProxy(
        NotificationService notificationService,
        MessageBroadcaster messageBroadcaster,
        EventLoopGroup eventLoopGroup,
        AtomicLong messageId,
        ProxyProvider provider,
        long id,
        Room room,
        String remoteRoom,
        String userId,
        CredentialsProvider credentialsProvider
    ) {
        super(eventLoopGroup, notificationService, provider, id, remoteRoom);

        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.room = room;
        this.userId = userId;
        this.bootstrap = createBootstrap(eventLoopGroup, remoteRoom, credentialsProvider, new Handler());
    }

    private static Bootstrap createBootstrap(
        EventLoopGroup eventLoopGroup,
        String channelName,
        CredentialsProvider tokenProvider,
        Handler handler
    ) {
        URI uri = URI.create("ws://chat.goodgame.ru:8081/chat/websocket");
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        JsonCodec jsonCodec = new JsonCodec();
        GoodGameCodec goodGameCodec = new GoodGameCodec();
        GoodGameProtocolHandler goodGameProtocolHandler = new GoodGameProtocolHandler(channelName, tokenProvider);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel c) throws Exception {
                ChannelPipeline pipeline = c.pipeline();
                pipeline.addLast("http-codec", new HttpClientCodec(4096, 8192, 8192));
                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast(new IdleStateHandler(120, 0, 140, TimeUnit.SECONDS));
                pipeline.addLast(new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, null, false, null, 4096));
                pipeline.addLast(jsonCodec);
                pipeline.addLast(goodGameCodec);
                pipeline.addLast(goodGameProtocolHandler);
                pipeline.addLast(handler);
            }
        });
        return bootstrap;
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        if (type == ModerationOperation.BAN) {
            String id = idCache.getIfPresent(name);
            if (id != null && !id.equals(userId)) {
                channel.writeAndFlush(new GoodGameEvent(GoodGameEventType.BAN, remoteRoom(), null, null, null, id));
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void onMessage(Message message) {

    }

    @Override
    public boolean outboundEnabled() {
        return false;
    }

    @Override
    public boolean moderationEnabled() {
        return userId != null;
    }

    @Override
    protected void connect() {
        ChannelFuture channelFuture = bootstrap.connect(HOST_NAME, 8081);
        channel = channelFuture.channel();
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                fail("failed to connect");
            }
        });
    }

    @Override
    protected void disconnect() {
        if (this.channel.isActive()) {
            channel.close();
        }
    }

    @ChannelHandler.Sharable
    private class Handler extends SimpleChannelInboundHandler<GoodGameEvent> {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("connected");
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.info("disconnected");
            if (state() == ProxyState.RUNNING) {
                minorFail("disconnected");
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (cause instanceof ProxyTokenException) {
                logger.error("exception", cause);
                fatalError(cause.getMessage());
            } else if (cause instanceof IOException) {
                logger.warn("exception", cause);
                minorFail(cause.getMessage());
            } else {
                logger.error("exception", cause);
                fail(cause.getMessage());
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, GoodGameEvent msg) throws Exception {
            if (msg.getType() == GoodGameEventType.FAILED_AUTH) {
                fail("failed login");
                channel.close();
            } else if (msg.getType() == GoodGameEventType.FAILED_JOIN) {
                fail("failed join");
                channel.close();
            } else if (msg.getType() == GoodGameEventType.BAD_RIGHTS) {
                fail("bad rights");
                channel.close();
            } else if (msg.getType() == GoodGameEventType.SUCCESS_JOIN) {
                channelName = msg.getChannelName();
                started();
            } else if (msg.getType() == GoodGameEventType.MESSAGE) {
                idCache.put(msg.getUser(), msg.getId());
                Message message = Message.extMessage(
                    room.getName(),
                    msg.getUser(),
                    LocalRole.USER,
                    GlobalRole.USER,
                    Colors.generateColor(msg.getUser()),
                    messageId.getAndIncrement(),
                    System.currentTimeMillis(),
                    msg.getText(),
                    "goodgame",
                    msg.getChannel(),
                    channelName
                );
                messageBroadcaster.submitMessage(message, room.FILTER);
            } else if (msg.getType() == GoodGameEventType.USER_BAN) {
                Message message = Message.proxyClear("#main", "goodgame", "GoodGame", msg.getUser());
                messageBroadcaster.submitMessage(message, room.FILTER);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent e = (IdleStateEvent) evt;
                if (e.state() == IdleState.READER_IDLE) {
                    ctx.writeAndFlush(new PingWebSocketFrame());
                } else if (e.state() == IdleState.ALL_IDLE) {
                    minorFail("idle");
                }
            }
        }
    }
}
