package lexek.wschat.proxy.sc2tv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lexek.netty.handler.codec.engineio.EngineIoDecoder;
import lexek.netty.handler.codec.engineio.EngineIoEncoder;
import lexek.netty.handler.codec.engineio.EngineIoProtocolHandler;
import lexek.netty.handler.codec.socketio.SocketIoDecoder;
import lexek.netty.handler.codec.socketio.SocketIoEncoder;
import lexek.netty.handler.codec.socketio.SocketIoPacket;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class Sc2tvChatProxy extends AbstractProxy {
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final Room room;
    private final Bootstrap bootstrap;
    private volatile Channel channel;

    protected Sc2tvChatProxy(
        NotificationService notificationService, String remoteRoom, MessageBroadcaster messageBroadcaster,
        EventLoopGroup eventLoopGroup, AtomicLong messageId, Room room, long id, ProxyProvider provider
    ) {
        super(eventLoopGroup, notificationService, provider, id, remoteRoom);
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.room = room;
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
        URI uri = URI.create("ws://funstream.tv/socket.io/?EIO=3&transport=websocket");
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel c) throws Exception {
                ChannelPipeline pipeline = c.pipeline();
                pipeline.addLast("http-codec", new HttpClientCodec(4096, 8192, 8192));
                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast(new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, null, false, null, 4096));
                pipeline.addLast(new EngineIoDecoder());
                pipeline.addLast(new EngineIoEncoder());
                pipeline.addLast(new EngineIoProtocolHandler());
                pipeline.addLast(new SocketIoDecoder());
                pipeline.addLast(new SocketIoEncoder());
                pipeline.addLast(new Sc2tvCodec());
                pipeline.addLast(handler);
            }
        });
        return bootstrap;
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

    @Override
    protected void connect() {
        ChannelFuture channelFuture = bootstrap.connect("funstream.tv", 80);
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
    private class Sc2ChannelHandler extends ChannelInboundHandlerAdapter {
        private final Set<Long> receivedMessages = new HashSet<>();
        private long eventId = 0;
        private Long joinEventId = null;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            logger.trace("{}", msg);
            if (msg instanceof SocketIoPacket) {
                SocketIoPacket socketIoPacket = (SocketIoPacket) msg;
                switch (socketIoPacket.getType()) {
                    case CONNECT: {
                        ObjectNode object = JsonNodeFactory.instance.objectNode();
                        object.put("channel", "stream/" + remoteRoom());
                        joinEventId = eventId++;
                        ctx.writeAndFlush(new Sc2tvMessage(joinEventId, "/chat/join", object));
                        break;
                    }
                }
            }
            if (msg instanceof Sc2tvMessage) {
                Sc2tvMessage sc2tvMessage = (Sc2tvMessage) msg;
                if (sc2tvMessage.getScope().equals("/chat/message")) {
                    JsonNode message = sc2tvMessage.getData();
                    Long sc2tvId = message.get("id").asLong();
                    if (!receivedMessages.contains(sc2tvId)) {
                        String userName = message.get("from").get("name").asText();
                        String text = message.get("text").asText();
                        JsonNode to = message.get("to");
                        String channel = message.get("channel").asText();
                        if (!to.isNull()) {
                            text = "@" + to.get("name").asText() + " " + text;
                        }
                        Message out = Message.extMessage(
                            room.getName(),
                            userName,
                            LocalRole.USER,
                            GlobalRole.USER,
                            Colors.generateColor(userName),
                            messageId.getAndIncrement(),
                            System.currentTimeMillis(),
                            ImmutableList.of(MessageNode.textNode(text)),
                            "sc2tv",
                            channel,
                            "sc2tv"
                        );
                        messageBroadcaster.submitMessage(out, room.FILTER);
                        receivedMessages.add(sc2tvId);
                    }
                }
            }
            if (msg instanceof Sc2tvAck) {
                Sc2tvAck ack = ((Sc2tvAck) msg);
                if (ack.getId() == joinEventId) {
                    String status = ack.getData().get("status").asText();
                    if (status.equals("ok")) {
                        started();
                    } else {
                        fail("failed join");
                    }
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            joinEventId = null;
            if (state() == ProxyState.RUNNING) {
                minorFail("disconnected");
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof IOException) {
                logger.debug("exception", cause);
                minorFail(cause.getMessage());
            } else {
                logger.warn("exception", cause);
                fail(cause.getMessage());
            }
        }
    }
}
