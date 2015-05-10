package lexek.wschat.proxy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import lexek.wschat.chat.*;
import lexek.wschat.services.AbstractService;
import lexek.wschat.util.Colors;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Sc2tvChatProxy extends AbstractService<Void> {
    private final JsonParser jsonParser = new JsonParser();
    private final String channel;
    private final MessageBroadcaster messageBroadcaster;
    private final EventLoopGroup eventLoopGroup;
    private final AtomicLong messageId;
    private final Room room;
    private Bootstrap bootstrap;

    protected Sc2tvChatProxy(String channel,
                             MessageBroadcaster messageBroadcaster,
                             EventLoopGroup eventLoopGroup,
                             AtomicLong messageId, Room room) {
        super("sc2tv", ImmutableList.<String>of());
        this.channel = channel;
        this.messageBroadcaster = messageBroadcaster;
        this.eventLoopGroup = eventLoopGroup;
        this.messageId = messageId;
        this.room = room;
    }

    @Override
    protected void start0() {
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            this.bootstrap.channel(EpollSocketChannel.class);
        } else {
            this.bootstrap.channel(NioSocketChannel.class);
        }
        this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final ChannelHandler handler = new Sc2ChannelHandler();
        this.bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel c) throws Exception {
                ChannelPipeline pipeline = c.pipeline();
                pipeline.addLast("http-codec", new HttpClientCodec(4096, 8192, 8192));
                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast(handler);
            }
        });
        this.bootstrap.connect("chat.sc2tv.ru", 80);
    }

    @Override
    public void performAction(String action) {
    }

    @Override
    public void stop() {
    }

    @Sharable
    private class Sc2ChannelHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private long lastId;
        private boolean firstRun = true;
        private Date lastModified = null;
        private String etag = null;

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
            logger.debug("received response with status {}", response.getStatus());
            if (response.getStatus().equals(HttpResponseStatus.OK)) {
                lastModified = HttpHeaders.getDateHeader(response, HttpHeaders.Names.LAST_MODIFIED);
                etag = HttpHeaders.getHeader(response, HttpHeaders.Names.ETAG);
                String data = response.content().toString(CharsetUtil.UTF_8);
                JsonObject root = jsonParser.parse(data).getAsJsonObject();
                List<JsonElement> messages = Lists.reverse(Lists.newArrayList(root.getAsJsonArray("messages")));
                for (JsonElement element : messages) {
                    JsonObject message = element.getAsJsonObject();
                    Long id = Longs.tryParse(message.get("id").getAsString());
                    if (id != null && id > lastId) {
                        if (!firstRun) {
                            Message chatMessage = Message.extMessage(
                                    "#main",
                                    message.get("name").getAsString(),
                                    LocalRole.USER,
                                    GlobalRole.USER,
                                    Colors.generateColor(message.get("name").getAsString()),
                                    messageId.getAndIncrement(),
                                    System.currentTimeMillis(),
                                    message.get("message").getAsString(),
                                    "sc2tv.ru",
                                    "sc2tv"
                            );
                            messageBroadcaster.submitMessage(chatMessage, Connection.STUB_CONNECTION, room.FILTER);
                        }
                        lastId = id;
                    }
                }
                if (firstRun) {
                    firstRun = false;
                }
            }
            if (HttpHeaders.isKeepAlive(response)) {
                ctx.channel().eventLoop().schedule(() -> {
                    logger.debug("sending request for next update");
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
            bootstrap.connect("chat.sc2tv.ru", 80);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.warn("exception", cause);
            ctx.close();
        }

        private HttpRequest composeRequest() {
            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.GET,
                    "http://chat.sc2tv.ru/memfs/channel-" + channel + ".json");
            if (lastModified != null) {
                HttpHeaders.setDateHeader(request, HttpHeaders.Names.IF_MODIFIED_SINCE, lastModified);
            }
            if (etag != null) {
                HttpHeaders.setHeader(request, HttpHeaders.Names.IF_NONE_MATCH, etag);
            }
            HttpHeaders.setKeepAlive(request, true);
            HttpHeaders.setHost(request, "chat.sc2tv.ru");
            return request;
        }
    }
}
