package lexek.wschat.proxy.twitch;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class TwitchMessageHandler extends SimpleChannelInboundHandler<TwitchEventMessage> {
    private final JTVEventListener eventListener;
    private final String channel;

    public TwitchMessageHandler(JTVEventListener eventListener, String channel) {
        this.eventListener = eventListener;
        this.channel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //do authentication and join room
        ctx.write("TWITCHCLIENT 3\r\n");
        ctx.write("PASS blah\r\n");
        ctx.write("NICK justinfan1337\r\n");
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
        }
    }
}
