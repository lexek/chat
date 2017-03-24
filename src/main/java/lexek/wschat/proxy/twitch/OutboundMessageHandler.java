package lexek.wschat.proxy.twitch;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.proxy.ProxyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OutboundMessageHandler {
    private static final long FIVE_MINUTES = TimeUnit.MINUTES.toMillis(5);
    private static final AttributeKey<Long> lastMessageAttrKey = AttributeKey.valueOf("__last_message");
    private final Logger logger = LoggerFactory.getLogger(OutboundMessageHandler.class);
    private final Map<String, Channel> connections;
    private final Bootstrap outboundBootstrap;
    private final TwitchCredentialsService credentialsService;
    private final ScheduledExecutorService eventLoopGroup;
    private final ProxyDescriptor descriptor;
    private ScheduledFuture scheduledFuture;

    public OutboundMessageHandler(
        TwitchCredentialsService credentialsService,
        EventLoopGroup eventLoopGroup,
        Map<String, Channel> connections,
        ProxyDescriptor descriptor
    ) {
        this.connections = connections;
        this.credentialsService = credentialsService;
        this.eventLoopGroup = eventLoopGroup;
        this.descriptor = descriptor;

        this.outboundBootstrap = new Bootstrap();
        this.outboundBootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            this.outboundBootstrap.channel(EpollSocketChannel.class);
        } else {
            this.outboundBootstrap.channel(NioSocketChannel.class);
        }
        this.outboundBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        this.outboundBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final ChannelHandler stringEncoder = new StringEncoder(CharsetUtil.UTF_8);
        final ChannelHandler stringDecoder = new StringDecoder(CharsetUtil.UTF_8);
        final ChannelHandler handler = new Handler();
        this.outboundBootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                pipeline.addLast(stringEncoder);
                pipeline.addLast(stringDecoder);
                pipeline.addLast(handler);
            }
        });
    }

    public void shutdown() {
        this.scheduledFuture.cancel(false);
        this.connections.values().forEach(Channel::close);
    }

    public void onMessage(Message message) {
        if (message.getType() == MessageType.MSG && message.get(MessageProperty.ROOM).equals(descriptor.getRoom().getName())) {
            long userId = message.get(MessageProperty.USER_ID);
            String name = message.get(MessageProperty.NAME);
            UserCredentials userCredentials = credentialsService.getCredentials(userId, name);

            if (userCredentials != null) {
                String id = userCredentials.getId();
                Channel channel = connections.get(id.toLowerCase());
                if (channel == null || !channel.isActive()) {
                    try {
                        channel = createConnection(id, userCredentials.getToken());
                    } catch (InterruptedException e) {
                        logger.warn("", e);
                    }
                }
                if (channel != null) {
                    channel.attr(lastMessageAttrKey).set(System.currentTimeMillis());
                    String text = message
                        .get(MessageProperty.MESSAGE_NODES)
                        .stream()
                        .map(MessageNode::getText)
                        .collect(Collectors.joining());
                    channel.writeAndFlush("PRIVMSG #" + descriptor.getRemoteRoom() + " :" + text + "\r\n");
                }
            }
        }
    }


    private Channel createConnection(String id, String token) throws InterruptedException {
        Channel channel;
        logger.debug("creating connection for {}", id);
        ChannelFuture f = outboundBootstrap.connect("irc.twitch.tv", 6667).sync();
        channel = f.channel();
        channel.write("TWITCHCLIENT 2\r\n");
        channel.write("PASS oauth:" + token + "\r\n");
        channel.write("NICK " + id + "\r\n");
        channel.write("JOIN #" + channel + "\r\n");
        channel.flush();
        connections.put(id.toLowerCase(), channel);
        logger.debug("connection created");
        return channel;
    }

    public void start() {
        this.scheduledFuture = eventLoopGroup.scheduleAtFixedRate(new ConnectionCleanupTask(), 10, 5, TimeUnit.MINUTES);
    }


    @Sharable
    private class Handler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            logger.trace("received message {}", msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof IOException) {
                logger.debug("exception", cause);
            } else {
                logger.warn("exception", cause);
            }
        }
    }

    private class ConnectionCleanupTask implements Runnable {
        @Override
        public void run() {
            try {
                logger.debug("starting cleanup");
                for (Map.Entry<String, Channel> entry : connections.entrySet()) {
                    Channel channel = entry.getValue();
                    Long lastMessage = channel.attr(lastMessageAttrKey).get();
                    if (System.currentTimeMillis() - lastMessage > FIVE_MINUTES) {
                        channel.disconnect();
                        connections.remove(entry.getKey());
                        logger.debug("connection released for {}", entry.getKey());
                    } else if (!channel.isActive()) {
                        connections.remove(entry.getKey());
                        logger.debug("connection released for {}", entry.getKey());
                    }
                }
                logger.debug("cleanup complete");
            } catch (Exception e) {
                logger.error("exception while clean up", e);
            }
        }
    }
}
