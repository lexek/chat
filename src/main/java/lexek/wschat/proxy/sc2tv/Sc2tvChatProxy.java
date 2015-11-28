package lexek.wschat.proxy.sc2tv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Sc2tvChatProxy implements Proxy {
    private final Logger logger = LoggerFactory.getLogger(Sc2tvChatProxy.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String channelName;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final Room room;
    private final Bootstrap bootstrap;
    private final long id;
    private final ProxyProvider provider;
    private volatile ScheduledFuture scheduledFuture;
    private volatile Channel channel;
    private volatile ProxyState state = ProxyState.NEW;

    protected Sc2tvChatProxy(String channelName,
                             MessageBroadcaster messageBroadcaster,
                             EventLoopGroup eventLoopGroup,
                             AtomicLong messageId, Room room, long id, ProxyProvider provider) {
        this.channelName = channelName;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.room = room;
        this.id = id;
        this.provider = provider;
        this.bootstrap = createBootstrap(eventLoopGroup, new Sc2ChannelHandler());
    }

    private static Bootstrap createBootstrap(EventLoopGroup eventLoopGroup, ChannelHandler handler) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel c) throws Exception {
                ChannelPipeline pipeline = c.pipeline();
                pipeline.addLast("http-codec", new HttpClientCodec(4096, 8192, 8192));
                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast(handler);
            }
        });
        return bootstrap;
    }

    @Override
    public void start() {
        this.state = ProxyState.STARTING;
        this.channel = this.bootstrap.connect("chat.sc2tv.ru", 80).channel();
        this.state = ProxyState.RUNNING;
    }

    @Override
    public void stop() {
        this.state = ProxyState.STOPPING;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        this.channel.close();
        this.state = ProxyState.STOPPED;
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMessage(Message message) {
        //do nothing
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
        return false;
    }

    @Override
    public ProxyState state() {
        return this.state;
    }

    @Override
    public String lastError() {
        return null;
    }

    @Sharable
    private class Sc2ChannelHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private long lastId;
        private boolean firstRun = true;
        private Date lastModified = null;

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
            logger.trace("received response with status {}", response.getStatus());
            if (response.getStatus().equals(HttpResponseStatus.OK)) {
                lastModified = HttpHeaders.getDateHeader(response, HttpHeaders.Names.LAST_MODIFIED);
                String data = response.content().toString(CharsetUtil.UTF_8);
                JsonNode root = objectMapper.readTree(data);
                List<JsonNode> messages = Lists.reverse(Lists.newArrayList((root.get("messages").elements())));
                for (JsonNode message : messages) {
                    Long id = Longs.tryParse(message.get("id").asText());
                    if (id != null && id > lastId) {
                        if (!firstRun) {
                            Message chatMessage = Message.extMessage(
                                "#main",
                                message.get("name").asText(),
                                LocalRole.USER,
                                GlobalRole.USER,
                                Colors.generateColor(message.get("name").asText()),
                                messageId.getAndIncrement(),
                                System.currentTimeMillis(),
                                message.get("message").asText(),
                                "sc2tv",
                                "sc2tv"
                            );
                            messageBroadcaster.submitMessage(chatMessage, room.FILTER);
                        }
                        lastId = id;
                    }
                }
                if (firstRun) {
                    firstRun = false;
                }
            }
            if (HttpHeaders.isKeepAlive(response)) {
                scheduledFuture = ctx.channel().eventLoop().schedule(() -> {
                    logger.trace("sending request for next update");
                    ctx.writeAndFlush(composeRequest());
                }, 10, TimeUnit.SECONDS);
            } else {
                logger.debug("closing channel coz no keepalive");
                ctx.close();
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.debug("connected");
            ctx.writeAndFlush(composeRequest());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.debug("disconnected");
            if (state == ProxyState.RUNNING) {
                channel = bootstrap.connect("chat.sc2tv.ru", 80).channel();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof IOException) {
                logger.debug("exception", cause);
            } else {
                logger.warn("exception", cause);
            }
            ctx.close();
        }

        private HttpRequest composeRequest() {
            HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                "http://chat.sc2tv.ru/memfs/channel-" + channelName + ".json");
            if (lastModified != null) {
                HttpHeaders.setDateHeader(request, HttpHeaders.Names.IF_MODIFIED_SINCE, lastModified);
            }
            HttpHeaders.setKeepAlive(request, true);
            HttpHeaders.setHost(request, "chat.sc2tv.ru");
            return request;
        }
    }
}
