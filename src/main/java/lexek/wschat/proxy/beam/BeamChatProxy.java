package lexek.wschat.proxy.beam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
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
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.proxy.AbstractNettyProxy;
import lexek.wschat.proxy.ProxyDescriptor;
import lexek.wschat.proxy.ProxyState;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.Colors;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class BeamChatProxy extends AbstractNettyProxy {
    private final BeamDataProvider beamDataProvider;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private volatile URI uri;
    private Long channelId;

    public BeamChatProxy(
        ProxyDescriptor descriptor,
        BeamDataProvider beamDataProvider, NotificationService notificationService,
        MessageBroadcaster messageBroadcaster, EventLoopGroup eventLoopGroup, AtomicLong messageId
    ) {
        super(eventLoopGroup, notificationService, descriptor);
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.beamDataProvider = beamDataProvider;
    }

    @Override
    protected void init() throws Exception {
        channelId = beamDataProvider.getId(remoteRoom());
        SslContext sslContext = SslContextBuilder.forClient().build();
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel c) throws Exception {
                ChannelPipeline pipeline = c.pipeline();
                pipeline.addLast(sslContext.newHandler(c.alloc()));
                pipeline.addLast("http-codec", new HttpClientCodec(4096, 8192, 8192));
                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast(new IdleStateHandler(120, 0, 140, TimeUnit.SECONDS));
                pipeline.addLast(new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, null, false, null, 4096));
                pipeline.addLast(new BeamCodec());
                pipeline.addLast(new BeamChannelHandler());
            }
        });
    }

    @Override
    protected void connect() throws Exception {
        String server = beamDataProvider.getChatServer(channelId);
        this.uri = URI.create(server);
        ChannelFuture channelFuture = bootstrap.connect(uri.getHost(), uri.getPort());
        channel = channelFuture.channel();
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                fail("failed to connect");
            }
        });
    }

    private class BeamChannelHandler extends SimpleChannelInboundHandler<JsonNode> {
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
                minorFail(cause.getMessage());
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, JsonNode message) throws Exception {
            String type = message.get("type").asText();
            if (type.equals("event")) {
                String event = message.get("event").asText();
                if (event.equals("ChatMessage")) {
                    JsonNode data = message.get("data");
                    String name = data.get("user_name").asText();
                    Long channel = data.get("channel").asLong();

                    StringBuilder messageBuilder = new StringBuilder();
                    for (JsonNode messageNode : data.get("message").get("message")) {
                        String nodeType = messageNode.get("type").asText();
                        if (nodeType.equals("link")) {
                            messageBuilder.append(messageNode.get("url").asText());
                        } else {
                            messageBuilder.append(messageNode.get("text").asText());
                        }
                    }

                    Room room = descriptor.getRoom();
                    Message chatMessage = Message.extMessage(
                        room.getName(),
                        name,
                        LocalRole.USER,
                        GlobalRole.USER,
                        Colors.generateColor(name),
                        messageId.getAndIncrement(),
                        System.currentTimeMillis(),
                        ImmutableList.of(MessageNode.textNode(messageBuilder.toString())),
                        "beam",
                        String.valueOf(channel),
                        remoteRoom()
                    );
                    messageBroadcaster.submitMessage(chatMessage, room.FILTER);
                }
            }
            if (type.equals("reply")) {
                long id = message.get("id").asLong();
                if (id == 0) {
                    if (message.get("error").isNull()) {
                        started();
                    } else {
                        fail(message.get("error").asText());
                    }
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
                ObjectNode rootNode = nodeFactory.objectNode();
                rootNode.put("type", "method");
                rootNode.put("method", "auth");
                rootNode.put("id", 0);
                ArrayNode arguments = nodeFactory.arrayNode();
                arguments.add(channelId);
                rootNode.set("arguments", arguments);
                ctx.writeAndFlush(rootNode);
            }
        }
    }
}
