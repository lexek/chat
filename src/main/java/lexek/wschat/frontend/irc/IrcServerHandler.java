package lexek.wschat.frontend.irc;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lexek.wschat.chat.*;
import lexek.wschat.security.AuthenticationCallback;
import lexek.wschat.security.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class IrcServerHandler extends ChannelInboundHandlerAdapter implements AuthenticationCallback<IrcConnection> {
    private static final AttributeKey<IrcConnection> WRAPPER_ATTR_KEY = AttributeKey.valueOf("IRC_CONNECTION_WRAPPER");

    private final Logger logger = LoggerFactory.getLogger(IrcServerHandler.class);
    private final MessageReactor messageReactor;
    private final String host;
    private final AuthenticationService authenticationService;
    private final IrcConnectionGroup connectionGroup;
    private final RoomManager roomManager;
    private final IrcProtocol protocol;

    public IrcServerHandler(MessageReactor messageReactor, String host, AuthenticationService authenticationService,
                            IrcConnectionGroup connectionGroup, RoomManager roomManager, IrcProtocol protocol) {
        this.messageReactor = messageReactor;
        this.host = host;
        this.authenticationService = authenticationService;
        this.connectionGroup = connectionGroup;
        this.roomManager = roomManager;
        this.protocol = protocol;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        IrcConnection connection = new IrcConnection(protocol, ctx.channel());
        ctx.attr(WRAPPER_ATTR_KEY).set(connection);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        IrcConnection connection = ctx.attr(WRAPPER_ATTR_KEY).get();
        if (connection.getState() == ConnectionState.AUTHENTICATED) {
            roomManager.partAll(connection, true);
            connectionGroup.deregisterConnection(connection);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        IrcConnection connection = ctx.attr(WRAPPER_ATTR_KEY).get();
        onMessage(connection, (String) message);
    }

    public void onMessage(final IrcConnection connection, String message) {
        if (connection.getState() == ConnectionState.AUTHENTICATED) {
            Message msg = connection.getCodec().decode(message);
            if (msg.getType() == MessageType.PING) {
                connection.send(Message.pongMessage(msg.getText()));
            } else {
                messageReactor.processMessage(connection, msg);
            }
        } else if (connection.getState() == ConnectionState.CONNECTED) {
            final Message msg = connection.getCodec().decode(message);
            switch (msg.getType()) {
                case PASS:
                    connection.setPassword(msg.getText());
                    break;
                case NICK:
                    String username = msg.getText();
                    if (connection.getPassword() == null) {
                        connection.close();
                    } else {
                        authenticationService.authenticateWithPassword(connection, username, connection.getPassword(), this);
                    }
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.writeAndFlush("PING\r\n");
            } else if (e.state() == IdleState.ALL_IDLE) {
                ctx.writeAndFlush(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void authenticationComplete(IrcConnection connection) {
        User u = connection.getUser();
        if (u.getRole() != GlobalRole.UNAUTHENTICATED) {
            connectionGroup.registerConnection(connection);
        } else {
            connection.close();
            return;
        }
        String username = u.getName();
        logger.debug("{}[{}] joined;", u.getName(), u.getRole());
        connection.send(":" + host + " 001 " + username + " :connected to yobaChat!");
        connection.send(":" + host + " 002 " + username + " :your host is yobaChat");
        connection.send(":" + host + " 003 " + username + " :This server was created on Tue Feb  9 20:05:44 2010");
        connection.send(":" + host + " 004 " + username + " chat.yoba.vg 1.0 w n");
        connection.send(":" + host + " 376 " + username + " end of motd");
    }

    @Override
    public void captchaRequired(IrcConnection connection, String name, long captchaId) {
        connection.send(":server NOTICE " + name + " :https://" + host + ":1337/recaptcha/" + captchaId);
    }
}