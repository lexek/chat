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
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.proxy.ProxyState;
import lexek.wschat.util.Colors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class GoodGameChatProxy implements Proxy {
    private static final String HOST_NAME = "chat.goodgame.ru";
    private final Logger logger = LoggerFactory.getLogger(GoodGameChatProxy.class);
    private final Cache<String, String> idCache = CacheBuilder.newBuilder().maximumSize(100).build();
    private final String channelName;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final Room room;
    private final Bootstrap bootstrap;
    private final long id;
    private final ProxyProvider provider;
    private final String userId;
    private volatile Channel channel;
    private volatile ProxyState state = ProxyState.NEW;
    private volatile String lastError;

    public GoodGameChatProxy(EventLoopGroup eventLoopGroup, String channelName, String name, String token,
                             MessageBroadcaster messageBroadcaster,
                             AtomicLong messageId, Room room, long id, ProxyProvider provider) {
        this.channelName = channelName;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.room = room;
        this.id = id;
        this.provider = provider;
        this.userId = name;
        this.bootstrap = createBootstrap(eventLoopGroup, channelName, name, token, new Handler());
    }

    private static Bootstrap createBootstrap(
        EventLoopGroup eventLoopGroup,
        String channelName,
        String username,
        String password,
        Handler handler
    ) {
        URI uri_ = null;
        try {
            uri_ = new URI("ws://chat.goodgame.ru:8081/chat/websocket");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        final URI uri = uri_;
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final JsonCodec jsonCodec = new JsonCodec();
        final GoodGameCodec goodGameCodec = new GoodGameCodec();
        final GoodGameProtocolHandler goodGameProtocolHandler = new GoodGameProtocolHandler(channelName, username, password);
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
    public void start() {
        this.state = ProxyState.STARTING;
        this.lastError = null;
        this.channel = this.bootstrap.connect(HOST_NAME, 8081).channel();
        this.state = ProxyState.RUNNING;
    }

    @Override
    public void stop() {
        this.state = ProxyState.STOPPING;
        this.channel.close();
        this.state = ProxyState.STOPPED;
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        if (type == ModerationOperation.BAN) {
            String id = idCache.getIfPresent(name);
            if (id != null && !id.equals(userId)) {
                channel.writeAndFlush(new GoodGameEvent(GoodGameEventType.BAN, channelName, null, null, id));
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void onMessage(Message message) {

    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public ProxyProvider provider() {
        return this.provider;
    }

    @Override
    public String remoteRoom() {
        return this.channelName;
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
    public ProxyState state() {
        return this.state;
    }

    @Override
    public String lastError() {
        return this.lastError;
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
            if (state == ProxyState.RUNNING) {
                channel = bootstrap.connect(HOST_NAME, 8081).channel();
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, GoodGameEvent msg) throws Exception {
            if (msg.getType() == GoodGameEventType.FAILED_AUTH) {
                logger.warn("failed login {}", userId);
                state = ProxyState.FAILED;
                lastError = "failed login";
                channel.close();
            } else if (msg.getType() == GoodGameEventType.FAILED_JOIN) {
                logger.warn("failed join {}", channelName);
                state = ProxyState.FAILED;
                lastError = "failed join";
                channel.close();
            } else if (msg.getType() == GoodGameEventType.BAD_RIGHTS) {
                logger.warn("bad rights {}: {}", channelName, userId);
                state = ProxyState.FAILED;
                lastError = "bad rights";
                channel.close();
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
                    "GoodGame"
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
                    ctx.close();
                }
            }
        }
    }
}
