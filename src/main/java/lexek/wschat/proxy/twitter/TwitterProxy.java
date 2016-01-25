package lexek.wschat.proxy.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateHandler;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.proxy.AbstractProxy;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.Colors;
import lexek.wschat.util.OAuthUtil;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class TwitterProxy extends AbstractProxy {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final Room room;
    private final Bootstrap bootstrap;
    private final String consumerKey;
    private final String consumerSecret;
    private final String accessToken;
    private final String accessTokenSecret;
    private volatile Channel channel;

    protected TwitterProxy(
        NotificationService notificationService,
        MessageBroadcaster messageBroadcaster,
        EventLoopGroup eventLoopGroup,
        ProxyProvider provider,
        long id,
        String remoteRoom,
        AtomicLong messageId,
        Room room,
        String consumerKey,
        String consumerSecret,
        String accessToken,
        String accessTokenSecret
    ) throws SSLException {
        super(eventLoopGroup, notificationService, provider, id, remoteRoom);
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.room = room;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.accessToken = accessToken;
        this.accessTokenSecret = accessTokenSecret;
        this.bootstrap = createBootstrap(eventLoopGroup, new Handler());
    }

    private static Bootstrap createBootstrap(EventLoopGroup eventLoopGroup, Handler handler) throws SSLException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        SslContext sslContext = SslContextBuilder.forClient().build();
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel c) throws Exception {
                ChannelPipeline pipeline = c.pipeline();
                pipeline.addLast(sslContext.newHandler(c.alloc()));
                pipeline.addLast(new HttpClientCodec(4096, 8192, 8192));
                pipeline.addLast(new IdleStateHandler(90, 0, 0));
                pipeline.addLast(new MessageToMessageDecoder<HttpContent>() {
                    @Override
                    protected void decode(ChannelHandlerContext channelHandlerContext, HttpContent httpContent, List<Object> out) throws Exception {
                        out.add(httpContent.content().retain());
                    }
                });
                pipeline.addLast(new DelimiterBasedFrameDecoder(102400, Unpooled.copiedBuffer("\r\n", StandardCharsets.UTF_8)));
                pipeline.addLast(handler);
            }
        });
        return bootstrap;
    }

    @Override
    protected void connect() {
        ChannelFuture channelFuture = bootstrap.connect("stream.twitter.com", 443);
        channel = channelFuture.channel();
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                fail("failed to connect");
            }
        });
    }

    @Override
    protected void disconnect() {
        this.channel.close();
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
    public boolean outboundEnabled() {
        return false;
    }

    @Override
    public boolean moderationEnabled() {
        return false;
    }

    @ChannelHandler.Sharable
    private class Handler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            String url = "https://stream.twitter.com/1.1/statuses/filter.json";
            Map<String, String> queryParameters = ImmutableMap.of(
                "track", "@" + remoteRoom()
            );
            QueryStringEncoder queryStringEncoder = new QueryStringEncoder(url);
            queryParameters.forEach(queryStringEncoder::addParam);
            String wholeUrl = queryStringEncoder.toUri().toString();
            HttpMethod method = HttpMethod.POST;
            FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                wholeUrl
            );
            String header = OAuthUtil.generateAuthorizationHeader(
                consumerKey,
                consumerSecret,
                accessToken,
                accessTokenSecret,
                url,
                method,
                queryParameters
            );
            request.headers().add("Authorization", header);
            ctx.writeAndFlush(request);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpResponse) {
                HttpResponse response = ((HttpResponse) msg);
                if (response.getStatus().code() == 200) {
                    started();
                } else {
                    fail(response.getStatus().reasonPhrase());
                }
            }
            if (msg instanceof ByteBuf) {
                String message = (((ByteBuf) msg).toString(StandardCharsets.UTF_8));
                if (!message.isEmpty()) {
                    JsonNode rootNode = objectMapper.readTree(message);
                    String name = rootNode.get("user").get("screen_name").asText();
                    String text = rootNode.get("text").asText();
                    Message out = Message.extMessage(
                        room.getName(),
                        name,
                        LocalRole.USER,
                        GlobalRole.USER,
                        Colors.generateColor(name),
                        messageId.getAndIncrement(),
                        System.currentTimeMillis(),
                        text,
                        "twitter",
                        "twitter"
                    );
                    messageBroadcaster.submitMessage(out, room.FILTER);
                }

            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.warn("exception", cause);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt == IdleState.READER_IDLE) {
                minorFail("timeout");
            }
        }
    }
}
