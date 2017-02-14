package lexek.wschat.frontend.ws;

import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lexek.wschat.chat.MessageReactor;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.chat.model.*;
import lexek.wschat.security.AuthenticationCallback;
import lexek.wschat.security.AuthenticationService;
import lexek.wschat.util.Constants;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Service
@ChannelHandler.Sharable
public class WebSocketChatHandler extends SimpleChannelInboundHandler<WebSocketFrame>
    implements AuthenticationCallback<WebSocketConnectionAdapter> {
    private static final AttributeKey<WebSocketConnectionAdapter> WRAPPER_ATTR_KEY =
        AttributeKey.valueOf("WEBSOCKET_CONNECTION_WRAPPER");
    private static final AttributeKey<String> SID_ATTR_KEY = AttributeKey.valueOf("WEBSOCKET_SID");

    private final Logger logger = LoggerFactory.getLogger(WebSocketChatHandler.class);
    private final AuthenticationService authenticationService;
    private final MessageReactor messageReactor;
    private final WebSocketProtocol protocol;
    private final JsonCodec codec;
    private final WebSocketConnectionGroup connectionGroup;
    private final RoomManager roomManager;

    @Inject
    public WebSocketChatHandler(
        AuthenticationService authenticationService,
        MessageReactor messageReactor,
        WebSocketProtocol protocol,
        JsonCodec codec, WebSocketConnectionGroup connectionGroup,
        RoomManager roomManager
    ) {
        this.authenticationService = authenticationService;
        this.messageReactor = messageReactor;
        this.protocol = protocol;
        this.codec = codec;
        this.connectionGroup = connectionGroup;
        this.roomManager = roomManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        WebSocketConnectionAdapter wrapper = new WebSocketConnectionAdapter(channel, protocol, codec);
        channel.attr(WRAPPER_ATTR_KEY).set(wrapper);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        WebSocketConnectionAdapter c = ctx.channel().attr(WRAPPER_ATTR_KEY).get();
        if (c != null) {
            if (c.getState() == ConnectionState.AUTHENTICATED) {
                roomManager.partAll(c, true);
                connectionGroup.deregisterConnection(c);
            }
            c.setState(ConnectionState.DISCONNECTED);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            handleTextMessage(ctx.channel(), ((TextWebSocketFrame) frame));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                logger.debug("reader_idle {} ", ctx.channel().toString());
                ctx.writeAndFlush(new PingWebSocketFrame());
            } else if (e.state() == IdleState.ALL_IDLE) {
                logger.debug("all_idle {} ", ctx.channel().toString());
                ctx.writeAndFlush(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
            }
        }
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketConnectionAdapter wrapper = ctx.channel().attr(WRAPPER_ATTR_KEY).get();
            wrapper.send(Message.protocolMessage(Constants.WEBSOCKET_PROTOCOL_VERSION));
        }
    }

    private void handleTextMessage(Channel channel, TextWebSocketFrame text) {
        WebSocketConnectionAdapter wrapper = channel.attr(WRAPPER_ATTR_KEY).get();
        Message decodedMessage = codec.decode(text);
        if (wrapper.getState() == ConnectionState.AUTHENTICATED) {
            if (decodedMessage.getType() == MessageType.PING) {
                wrapper.send(Message.emptyMessage(MessageType.PONG));
            } else if (decodedMessage.getType() == MessageType.LOGOUT) {
                String sid = channel.attr(SID_ATTR_KEY).get();
                if (sid != null) {
                    authenticationService.invalidateSession(sid);
                }
            } else {
                messageReactor.processMessage(wrapper, decodedMessage);
            }
        } else if (wrapper.getState() == ConnectionState.CONNECTED) {
            if (decodedMessage.getType() == MessageType.SESSION) {
                String sid = decodedMessage.get(MessageProperty.TEXT);
                authenticationService.authenticateWithSid(wrapper, sid, this);
                if (sid != null) {
                    channel.attr(SID_ATTR_KEY).set(sid);
                }
                return;
            }
            channel.writeAndFlush(codec.encode(Message.errorMessage("Not authenticated, try refreshing page.")));
        }
    }

    @Override
    public void authenticationComplete(WebSocketConnectionAdapter connection) {
        User user = connection.getUser();
        logger.debug("{}[{}] joined; ip: {}", user.getName(), user.getRole(), connection.getIp());
        connectionGroup.registerConnection(connection);
        if (!user.hasRole(GlobalRole.USER)) {
            connection.getChannel().attr(SID_ATTR_KEY).set(null);
        }
    }

    @Override
    public void captchaRequired(WebSocketConnectionAdapter connection, String name, long captchaId) {
        //not needed
    }
}