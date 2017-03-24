package lexek.wschat.proxy.goodgame;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.msg.DefaultMessageProcessingService;
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.proxy.*;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.Colors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class GoodGameChatProxy extends AbstractNettyProxy {
    private static final String HOST_NAME = "chat.goodgame.ru";
    private static final int PORT = 443;
    private final Cache<String, String> idCache = CacheBuilder.newBuilder().maximumSize(100).build();
    private final DefaultMessageProcessingService messageProcessingService;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final GoodGameApiClient goodGameApiClient;
    private final ProxyAuthService proxyAuthService;
    private Long channelId;
    private String userId;

    public GoodGameChatProxy(
        ProxyDescriptor descriptor,
        DefaultMessageProcessingService messageProcessingService,
        NotificationService notificationService,
        MessageBroadcaster messageBroadcaster,
        EventLoopGroup eventLoopGroup,
        AtomicLong messageId,
        GoodGameApiClient goodGameApiClient,
        ProxyAuthService proxyAuthService
    ) {
        super(eventLoopGroup, notificationService, descriptor);
        this.messageProcessingService = messageProcessingService;

        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.goodGameApiClient = goodGameApiClient;
        this.proxyAuthService = proxyAuthService;
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        if (type == ModerationOperation.BAN) {
            String id = idCache.getIfPresent(name);
            if (id != null && !id.equals(userId)) {
                channel.writeAndFlush(new GoodGameEvent(GoodGameEventType.BAN, String.valueOf(channelId), null, null, null, id));
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    protected void init() throws Exception {
        Optional<Long> authId = descriptor.getAuthId();
        if (authId.isPresent()) {
            SocialProfile profile = proxyAuthService.getProfile(authId.get());
            if (profile != null) {
                userId = profile.getId();
            }
        }
        channelId = goodGameApiClient.getChannelId(remoteRoom());

        URI uri = URI.create("wss://" + HOST_NAME + ":" + PORT + "/chat/websocket");
        JsonCodec jsonCodec = new JsonCodec();
        GoodGameCodec goodGameCodec = new GoodGameCodec();
        SslContext sslContext = SslContextBuilder.forClient().build();
        Handler handler = new Handler();
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel c) throws Exception {
                ChannelPipeline pipeline = c.pipeline();
                pipeline.addLast(sslContext.newHandler(c.alloc()));
                pipeline.addLast("http-codec", new HttpClientCodec(4096, 8192, 8192));
                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast(new IdleStateHandler(120, 0, 140, TimeUnit.SECONDS));
                pipeline.addLast(new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, null, false, null, 4096));
                pipeline.addLast(jsonCodec);
                pipeline.addLast(goodGameCodec);
                pipeline.addLast(handler);
            }
        });
    }

    @Override
    protected void connect() {
        ChannelFuture channelFuture = bootstrap.connect(HOST_NAME, 443);
        channel = channelFuture.channel();
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                fail("failed to connect");
            }
        });
    }

    @ChannelHandler.Sharable
    private class Handler extends SimpleChannelInboundHandler<GoodGameEvent> {
        private static final String PROTOCOL_VERSION = "1.1";
        private String lastUser;

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
            switch (msg.getType()) {
                case WELCOME:
                    if (msg.getText().equals(PROTOCOL_VERSION)) {
                        logger.debug("protocol versions match");
                    } else {
                        logger.warn("different protocol version");
                    }
                    String password = null;
                    String name = null;
                    Optional<Long> authId = descriptor.getAuthId();
                    if (authId.isPresent()) {
                        Credentials credentials = goodGameApiClient.getCredentials(authId.get());
                        name = credentials.getUserId();
                        password = credentials.getToken();
                    }
                    lastUser = name;
                    ctx.writeAndFlush(new GoodGameEvent(GoodGameEventType.AUTH, null, null, password, name, null));
                    break;
                case SUCCESS_AUTH:
                    if (lastUser == null || msg.getUser().equals(lastUser)) {
                        ctx.writeAndFlush(new GoodGameEvent(GoodGameEventType.JOIN, String.valueOf(channelId), null, null, null, null));
                    } else {
                        fail("failed login");
                    }
                    break;
                case SUCCESS_JOIN:
                    started();
                    break;
                case FAILED_JOIN:
                    fail("failed join");
                    break;
                case BAD_RIGHTS:
                    fail("bad rights");
                    break;
                case MESSAGE: {
                    idCache.put(msg.getUser(), msg.getId());
                    Room room = descriptor.getRoom();
                    Message message = Message.extMessage(
                        room.getName(),
                        msg.getUser(),
                        LocalRole.USER,
                        GlobalRole.USER,
                        Colors.generateColor(msg.getUser()),
                        messageId.getAndIncrement(),
                        System.currentTimeMillis(),
                        parseMessage(msg.getText()),
                        "goodgame",
                        channelId.toString(),
                        remoteRoom()
                    );
                    messageBroadcaster.submitMessage(message, room.FILTER);
                    break;
                }
                case USER_BAN: {
                    Room room = descriptor.getRoom();
                    messageBroadcaster.submitMessage(
                        Message.proxyClear(
                            room.getName(),
                            "goodgame",
                            channelId.toString(),
                            msg.getUser()
                        ),
                        room.FILTER
                    );
                    break;
                }
                case ERROR:
                    logger.debug("error {}", msg.getText());
                    break;
                default:
                    logger.debug("unsupported message type {}", msg.getType());
                    break;
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

    private List<MessageNode> parseMessage(String text) {
        List<MessageNode> body = new LinkedList<>();

        Element element = Jsoup.parseBodyFragment(text).body();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                body.add(MessageNode.textNode(((TextNode) node).text()));
            } else if (node instanceof Element) {
                Element e = ((Element) node);
                if ("a".equals(e.tagName())) {
                    body.add(MessageNode.urlNode(e.attr("href")));
                } else {
                    logger.warn("unknown tag {}", e.tagName());
                }
            } else {
                logger.warn("unknown node {}", node);
            }
        }
        messageProcessingService.processMessage(body, true);
        return body;
    }
}
