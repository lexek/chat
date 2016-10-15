package lexek.wschat.proxy.cybergame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.proxy.AbstractProxy;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.proxy.ProxyState;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.Colors;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CybergameTvChatProxy extends AbstractProxy {
    private final CybergameTvApiClient apiClient;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final Room room;
    private final Bootstrap bootstrap;
    private volatile Channel channel;
    private String channelId;

    public CybergameTvChatProxy(
        CybergameTvApiClient apiClient, NotificationService notificationService, MessageBroadcaster messageBroadcaster,
        EventLoopGroup eventLoopGroup, AtomicLong messageId, ProxyProvider provider, Room room, String remoteRoom,
        long id
    ) {
        super(eventLoopGroup, notificationService, provider, id, remoteRoom);
        this.apiClient = apiClient;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.room = room;
        this.bootstrap = createBootstrap(eventLoopGroup, new CybergameTvChannelHandler());
    }

    private static Bootstrap createBootstrap(
        EventLoopGroup eventLoopGroup,
        ChannelHandler handler
    ) {
        URI uri = URI.create("ws://newchat.cybergame.tv:9002/");
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel c) throws Exception {
                ChannelPipeline pipeline = c.pipeline();
                pipeline.addLast("http-codec", new HttpClientCodec(4096, 8192, 8192));
                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast(new IdleStateHandler(120, 0, 140, TimeUnit.SECONDS));
                pipeline.addLast(new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, null, false, null, 4096));
                pipeline.addLast(new CybergameTvMessageCodec());
                pipeline.addLast(handler);
            }
        });
        return bootstrap;
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        throw new UnsupportedOperationException(type.toString());
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

    @Override
    protected void connect() {
        if (channelId == null) {
            try {
                channelId = apiClient.getChannelId(remoteRoom());
            } catch (Exception e) {
                logger.error("unable to fetch channel id", e);
                fail("unable to fetch channel id");
                return;
            }
        }
        ChannelFuture channelFuture = bootstrap.connect("newchat.cybergame.tv", 9002);
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
            this.channel.close();
        }
    }

    @Sharable
    private class CybergameTvChannelHandler extends SimpleChannelInboundHandler<CybergameTvEvent> {
        private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (state() == ProxyState.RUNNING) {
                minorFail("disconnected");
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof IOException) {
                logger.warn("exception", cause);
                minorFail(cause.getMessage());
            } else {
                logger.error("exception", cause);
                fail(cause.getMessage());
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, CybergameTvEvent message) throws Exception {
            JsonNode data = message.getData();
            if (message.getType().equals("msg")) {
                String name = data.get("nickname").asText();
                List<MessageNode> messageBody = new ArrayList<>();
                for (JsonNode messageNode : data.get("message")) {
                    String type = messageNode.get("type").asText();
                    String text = messageNode.get("text").asText();
                    if ("emote".equals(type)) {
                        String image = messageNode.get("image").asText();
                        if (image.endsWith(".svg")) {
                            image = image.substring(0, image.length() - 4) + ".png";
                        }
                        messageBody.add(MessageNode.emoticonNode(text, "/emoticons/cybergame/" + image));
                    } else {
                        messageBody.add(MessageNode.textNode(text));
                    }
                }
                Message chatMessage = Message.extMessage(
                    room.getName(),
                    name,
                    LocalRole.USER,
                    GlobalRole.USER,
                    Colors.generateColor(name),
                    messageId.getAndIncrement(),
                    System.currentTimeMillis(),
                    messageBody,
                    "cybergame",
                    channelId,
                    remoteRoom()
                );
                messageBroadcaster.submitMessage(chatMessage, room.FILTER);
            }
            if (message.getType().equals("state")) {
                String channel = data.get("channel").asText();
                int state = data.get("state").asInt();
                if (channel.equals("channel:" + channelId) && state == 2) {
                    started();
                } else {
                    fail("join failed");
                }
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
            } else if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                ObjectNode data = nodeFactory.objectNode();
                data.put("cid", channelId);
                ctx.writeAndFlush(new CybergameTvEvent("join", data));
            }
        }
    }
}
