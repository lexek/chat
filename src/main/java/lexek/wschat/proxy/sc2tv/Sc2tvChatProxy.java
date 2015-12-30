package lexek.wschat.proxy.sc2tv;

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
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.proxy.ProxyState;
import lexek.wschat.util.Colors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class Sc2tvChatProxy implements Proxy {
    private final Logger logger = LoggerFactory.getLogger(Sc2tvChatProxy.class);
    private final String channelName;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final Room room;
    private final Bootstrap bootstrap;
    private final long id;
    private final ProxyProvider provider;
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
    public void start() {
        this.state = ProxyState.STARTING;
        this.channel = this.bootstrap.connect("funstream.tv", 80).channel();
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
    private class Sc2ChannelHandler extends ChannelInboundHandlerAdapter {
        private final Set<Long> receivedMessages = new HashSet<>();
        private long eventId = 0;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            logger.trace("{}", msg);
            if (msg instanceof SocketIoPacket) {
                SocketIoPacket socketIoPacket = (SocketIoPacket) msg;
                switch (socketIoPacket.getType()) {
                    case CONNECT:
                        ObjectNode object = JsonNodeFactory.instance.objectNode();
                        object.put("channel", "stream/" + channelName);
                        ctx.writeAndFlush(new Sc2tvMessage(eventId++, "/chat/join", object));
                        break;
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
                            text,
                            "sc2tv",
                            "sc2tv"
                        );
                        messageBroadcaster.submitMessage(out, room.FILTER);
                        receivedMessages.add(sc2tvId);
                    }
                }
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.debug("connected");
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.debug("disconnected");
            if (state == ProxyState.RUNNING) {
                channel = bootstrap.connect("funstream.tv", 80).channel();
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
    }
}
