package lexek.wschat.proxy.twitch;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class TwitchMessageHandler extends SimpleChannelInboundHandler<TwitchEventMessage> {
    private final JTVEventListener eventListener;
    private final String channel;
    private final String username;
    private final String token;

    public TwitchMessageHandler(JTVEventListener eventListener, String channel, String username, String token) {
        this.eventListener = eventListener;
        this.channel = channel;
        this.username = username;
        this.token = token;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //do authentication and join room
        if (username != null && token != null) {
            ctx.write("PASS oauth:" + token + "\r\n");
            ctx.write("NICK " + username + "\r\n");
        } else {
            ctx.write("PASS blah\r\n");
            ctx.write("NICK justinfan1337\r\n");
        }
        ctx.write("CAP REQ :twitch.tv/commands\r\n");
        ctx.writeAndFlush("JOIN #" + channel + "\r\n");

        eventListener.onConnected();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        eventListener.onDisconnected();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TwitchEventMessage msg) throws Exception {
        if (msg.getType() == TwitchEventMessage.Type.CLEAR) {
            eventListener.onClear(msg.getData());
        } else if (msg.getType() == TwitchEventMessage.Type.MSG && msg instanceof TwitchUserMessage) {
            eventListener.onMessage(((TwitchUserMessage) msg).getUser(), msg.getData());
        } else if (msg.getType() == TwitchEventMessage.Type.LOGIN_FAILED) {
            eventListener.loginFailed();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.writeAndFlush("PING\r\n");
            } else if (e.state() == IdleState.ALL_IDLE) {
                ctx.close();
            }
        }
    }
}
