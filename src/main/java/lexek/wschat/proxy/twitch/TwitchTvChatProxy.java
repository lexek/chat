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
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.proxy.AbstractProxy;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.proxy.ProxyState;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.Colors;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TwitchTvChatProxy extends AbstractProxy {
    private final String username;
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private final Room room;
    private final Bootstrap inboundBootstrap;
    private final Map<String, Channel> connections = new ConcurrentHashMapV8<>();
    private final OutboundMessageHandler outboundHandler;
    private volatile Channel channel;

    public TwitchTvChatProxy(
        NotificationService notificationService, long id, ProxyProvider provider, Room room, String remoteRoom,
        String username, String token, boolean outbound, AtomicLong messageId, MessageBroadcaster messageBroadcaster,
        AuthenticationManager authenticationManager, EventLoopGroup eventLoopGroup
    ) {
        super(eventLoopGroup, notificationService, provider, id, remoteRoom);
        this.username = username;
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.room = room;
        if (outbound) {
            this.outboundHandler =
                new OutboundMessageHandler(authenticationManager, eventLoopGroup, connections, remoteRoom);
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
                pipeline.addLast(new TwitchMessageHandler(eventListener, remoteRoom, username, token));
            }
        });
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        if (type == ModerationOperation.BAN) {
            channel.writeAndFlush("PRIVMSG #" + remoteRoom() + " :.ban " + name + "\r\n");
        }
        if (type == ModerationOperation.TIMEOUT) {
            channel.writeAndFlush("PRIVMSG #" + remoteRoom() + " :.timeout " + name + "\r\n");
        }
        if (type == ModerationOperation.UNBAN) {
            channel.writeAndFlush("PRIVMSG #" + remoteRoom() + " :.unban " + name + "\r\n");
        }
        if (type == ModerationOperation.CLEAR) {
            channel.writeAndFlush("PRIVMSG #" + remoteRoom() + " :.ban " + name + "\r\n");
            channel.writeAndFlush("PRIVMSG #" + remoteRoom() + " :.unban " + name + "\r\n");
        }
    }

    @Override
    public void onMessage(Message message) {
        if (this.outboundHandler != null) {
            outboundHandler.onMessage(message);
        }
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
    protected void connect() {
        ChannelFuture channelFuture = inboundBootstrap.connect("irc.twitch.tv", 6667);
        channel = channelFuture.channel();
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                fail("failed to connect");
            }
        });
        if (this.outboundHandler != null) {
            this.outboundHandler.start();
        }
    }

    @Override
    protected void disconnect() {
        if (this.channel.isActive()) {
            this.channel.close();
        }
        if (this.outboundHandler != null) {
            this.outboundHandler.shutdown();
        }
    }

    private class JtvEventListenerImpl implements JTVEventListener {
        @Override
        public void onDisconnected() {
            if (state() == ProxyState.RUNNING) {
                minorFail("disconnected");
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
                    remoteRoom()
                );
                messageBroadcaster.submitMessage(msg, room.FILTER);
            }
        }

        @Override
        public void onClear(String name) {
            Message msg = Message.proxyClear(
                "#main",
                "twitch",
                remoteRoom(),
                name
            );
            messageBroadcaster.submitMessage(msg, room.FILTER);
        }

        @Override
        public void loginFailed() {
            fail("login failed");
        }

        @Override
        public void selfJoined(String room) {
            if (room.equalsIgnoreCase(remoteRoom())) {
                started();
            } else {
                fail("wrong room, wut?");
            }
        }

        @Override
        public void exceptionCaught(Throwable throwable) {
            logger.warn("exception", throwable);
            minorFail(throwable.getMessage());
        }
    }
}
