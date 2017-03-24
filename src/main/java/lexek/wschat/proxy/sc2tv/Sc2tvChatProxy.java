package lexek.wschat.proxy.sc2tv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.socket.SocketChannel;
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
import lexek.wschat.chat.msg.MessageProcessingService;
import lexek.wschat.proxy.AbstractNettyProxy;
import lexek.wschat.proxy.ProxyDescriptor;
import lexek.wschat.proxy.ProxyState;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.Colors;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class Sc2tvChatProxy extends AbstractNettyProxy {
    private final Peka2TvApiClient apiClient;
    private final MessageBroadcaster messageBroadcaster;
    private final MessageProcessingService messageProcessingService;
    private final AtomicLong messageId;
    private Long streamId;

    protected Sc2tvChatProxy(
        ProxyDescriptor descriptor,
        NotificationService notificationService, MessageBroadcaster messageBroadcaster,
        EventLoopGroup eventLoopGroup, AtomicLong messageId,
        Peka2TvApiClient apiClient, MessageProcessingService messageProcessingService
    ) {
        super(eventLoopGroup, notificationService, descriptor);
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.apiClient = apiClient;
        this.messageProcessingService = messageProcessingService;
    }

    @Override
    protected void init() throws Exception {
        streamId = apiClient.getStreamId(remoteRoom());

        URI uri = URI.create("ws://funstream.tv/socket.io/?EIO=3&transport=websocket");
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
                pipeline.addLast(new Sc2ChannelHandler());
            }
        });
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
                        object.put("channel", "stream/" + streamId);
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
                        Room room = descriptor.getRoom();
                        Message out = Message.extMessage(
                            room.getName(),
                            userName,
                            LocalRole.USER,
                            GlobalRole.USER,
                            Colors.generateColor(userName),
                            messageId.getAndIncrement(),
                            System.currentTimeMillis(),
                            messageProcessingService.processMessage(text, true),
                            "sc2tv",
                            channel,
                            remoteRoom()
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
