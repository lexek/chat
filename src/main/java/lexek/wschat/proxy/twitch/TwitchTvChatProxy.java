package lexek.wschat.proxy.twitch;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.db.model.ProxyAuth;
import lexek.wschat.proxy.*;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.Colors;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class TwitchTvChatProxy extends AbstractProxy {
    private final Long proxyAuthId;
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private final Room room;
    private final Bootstrap inboundBootstrap;
    private final Map<String, Channel> connections = new ConcurrentHashMapV8<>();
    private final OutboundMessageHandler outboundHandler;
    private volatile Channel channel;

    public TwitchTvChatProxy(
        NotificationService notificationService,
        long id,
        ProxyProvider provider,
        Room room,
        String remoteRoom,
        Long proxyAuthId,
        boolean outbound,
        AtomicLong messageId,
        MessageBroadcaster messageBroadcaster,
        TwitchCredentialsService credentialsService,
        EventLoopGroup eventLoopGroup,
        ProxyAuthService proxyAuthService
    ) {
        super(eventLoopGroup, notificationService, provider, id, remoteRoom);
        this.proxyAuthId = proxyAuthId;
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.room = room;
        if (outbound) {
            this.outboundHandler =
                new OutboundMessageHandler(credentialsService, eventLoopGroup, connections, remoteRoom, room);
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
        String username = null;
        String token = null;
        if (proxyAuthId != null) {
            ProxyAuth auth = proxyAuthService.getAuth(proxyAuthId);
            username = auth.getExternalName();
            token = auth.getKey();
        }
        this.inboundBootstrap.handler(new InboundChannelInitializer(new JtvEventListenerImpl(), remoteRoom, username, token));
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
        return proxyAuthId != null;
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
        if (this.channel != null && this.channel.isActive()) {
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
        public void onMessage(String userName, String color, List<MessageNode> message) {
            if (!connections.containsKey(userName.toLowerCase())) {
                Message msg = Message.extMessage(
                    room.getName(),
                    userName,
                    LocalRole.USER,
                    GlobalRole.USER,
                    StringUtils.isEmpty(color) ? Colors.generateColor(userName) : color,
                    messageId.getAndIncrement(),
                    System.currentTimeMillis(),
                    message,
                    "twitch",
                    remoteRoom(),
                    remoteRoom()
                );
                messageBroadcaster.submitMessage(msg, room.FILTER);
            }
        }

        @Override
        public void onSub(String userName, String color, List<MessageNode> message, int months) {
            Message msg = Message.subMessage(
                room.getName(),
                userName,
                StringUtils.isEmpty(color) ? Colors.generateColor(userName) : color,
                message,
                "twitch",
                remoteRoom(),
                remoteRoom(),
                months
            );
            messageBroadcaster.submitMessage(msg, room.FILTER);
        }

        @Override
        public void onClear(String name) {
            Message msg = Message.proxyClear(
                room.getName(),
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
        public void exceptionCaught(Throwable cause) {
            if (cause instanceof IOException) {
                logger.warn("exception", cause);
                minorFail(cause.getMessage());
            } else {
                logger.error("exception", cause);
                fail(cause.getMessage());
            }
        }
    }
}
