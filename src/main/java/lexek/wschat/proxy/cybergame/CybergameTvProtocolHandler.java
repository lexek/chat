package lexek.wschat.proxy.cybergame;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class CybergameTvProtocolHandler extends SimpleChannelInboundHandler<CybergameTvInboundEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CybergameTvProtocolHandler.class);
    private final String channel;

    public CybergameTvProtocolHandler(String channel) {
        this.channel = channel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CybergameTvInboundEvent msg) throws Exception {
        switch (msg.getType()) {
            case AUTH:
                logger.debug("received auth event");
                ctx.writeAndFlush(new CybergameTvOutboundEvent(CybergameTvEventType.AUTH, channel));
                break;
            case MESSAGE:
                logger.debug("received message event");
                ctx.fireChannelRead(msg);
                break;
        }
    }
}
