package lexek.wschat.proxy.twitch;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.chat.*;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.proxy.ProxyState;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.util.Colors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TwitchTvChatProxy implements Proxy {
    private final Logger logger = LoggerFactory.getLogger(TwitchTvChatProxy.class);
    private final ProxyProvider provider;
    private final String channelName;
    private final String username;
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private final Room room;
    private final Bootstrap inboundBootstrap;
    private final Map<String, Channel> connections = new ConcurrentHashMapV8<>();
    private final OutboundMessageHandler outboundHandler;
    private final long id;
    private volatile Channel channel;
    private volatile ProxyState state = ProxyState.NEW;
    private volatile String lastError;

    public TwitchTvChatProxy(
        long id, ProxyProvider provider, Room room, String channelName, String username, String token, boolean outbound,
        AtomicLong messageId, MessageBroadcaster messageBroadcaster, AuthenticationManager authenticationManager,
        EventLoopGroup eventLoopGroup
    ) {
        this.channelName = channelName;
        this.username = username;
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.room = room;
        this.id = id;
        this.provider = provider;
        if (outbound) {
            this.outboundHandler = new OutboundMessageHandler(connections, channelName, authenticationManager, eventLoopGroup);
        } else {
            this.outboundHandler = null;
        }

        this.inboundBootstrap = new Bootstrap();
        this.inboundBootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            this.inboundBootstrap.channel(EpollSocketChannel.class);
        } else {
            this.inboundBootstrap.channel(NioSocketChannel.class);
        }
        this.inboundBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        this.inboundBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final ChannelHandler stringEncoder = new StringEncoder(CharsetUtil.UTF_8);
        final ChannelHandler stringDecoder = new StringDecoder(CharsetUtil.UTF_8);
        final JTVEventListener eventListener = new JtvEventListenerImpl();
        this.inboundBootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                pipeline.addLast(stringEncoder);
                pipeline.addLast(stringDecoder);
                pipeline.addLast(new IdleStateHandler(120, 0, 140, TimeUnit.SECONDS));
                pipeline.addLast(new TwitchTvMessageDecoder());
                pipeline.addLast(new TwitchMessageHandler(eventListener, channelName, username, token));
            }
        });
    }

    @Override
    public void start() {
        this.state = ProxyState.STARTING;
        this.channel = inboundBootstrap.connect("irc.twitch.tv", 6667).channel();
        this.lastError = null;
        if (this.outboundHandler != null) {
            this.outboundHandler.start();
        }
        this.state = ProxyState.RUNNING;
    }

    @Override
    public void stop() {
        this.state = ProxyState.STOPPING;
        this.channel.close();
        if (this.outboundHandler != null) {
            this.outboundHandler.shutdown();
        }
        this.state = ProxyState.STOPPED;
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        if (type == ModerationOperation.BAN) {
            channel.writeAndFlush("PRIVMSG #" + channelName + " :.ban " + name + "\r\n");
        }
        if (type == ModerationOperation.TIMEOUT) {
            channel.writeAndFlush("PRIVMSG #" + channelName + " :.timeout " + name + "\r\n");
        }
        if (type == ModerationOperation.UNBAN) {
            channel.writeAndFlush("PRIVMSG #" + channelName + " :.unban " + name + "\r\n");
        }
        if (type == ModerationOperation.CLEAR) {
            channel.writeAndFlush("PRIVMSG #" + channelName + " :.ban " + name + "\r\n");
            channel.writeAndFlush("PRIVMSG #" + channelName + " :.unban " + name + "\r\n");
        }
    }

    @Override
    public void onMessage(Connection connection, Message message) {
        if (this.outboundHandler != null) {
            outboundHandler.onMessage(connection, message);
        }
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
        return channelName;
    }

    @Override
    public boolean outboundEnabled() {
        return this.outboundHandler != null;
    }

    @Override
    public boolean moderationEnabled() {
        return username != null;
    }

    @Override
    public ProxyState state() {
        return this.state;
    }

    @Override
    public String lastError() {
        return this.lastError;
    }

    private class JtvEventListenerImpl implements JTVEventListener {
        @Override
        public void onConnected() {
            logger.info("Twitch proxy connected. Channel: {}", channelName);
        }

        @Override
        public void onDisconnected() {
            logger.info("Twitch proxy disconnected.");
            if (state == ProxyState.RUNNING) {
                channel = inboundBootstrap.connect("irc.twitch.tv", 6667).channel();
            }
        }

        @Override
        public void onMessage(TwitchUser user, String message) {
            if (!connections.containsKey(user.getNick().toLowerCase())) {
                Message msg = Message.extMessage(
                    room.getName(),
                    user.getNick(),
                    LocalRole.USER,
                    GlobalRole.USER,
                    Colors.generateColor(user.getNick()),
                    messageId.getAndIncrement(),
                    System.currentTimeMillis(),
                    message,
                    "twitch",
                    channelName
                );
                messageBroadcaster.submitMessage(msg, Connection.STUB_CONNECTION, room.FILTER);
            }
        }

        @Override
        public void onClear(String name) {
            Message msg = Message.proxyClear(
                "#main",
                "twitch",
                channelName,
                name
            );
            messageBroadcaster.submitMessage(msg, Connection.STUB_CONNECTION, room.FILTER);
        }

        @Override
        public void onServerMessage(String s) {
            logger.trace(s);
        }

        @Override
        public void loginFailed() {
            lastError = "login failed";
            state = ProxyState.FAILED;
            channel.close();
        }
    }
}
