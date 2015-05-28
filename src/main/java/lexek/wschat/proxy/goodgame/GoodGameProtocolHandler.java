package lexek.wschat.proxy.goodgame;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class GoodGameProtocolHandler extends SimpleChannelInboundHandler<GoodGameEvent> {
    private static final String PROTOCOL_VERSION = "1.1";

    private final Logger logger = LoggerFactory.getLogger(GoodGameProtocolHandler.class);
    private final String channelId;

    public GoodGameProtocolHandler(String channelId) {
        this.channelId = channelId;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GoodGameEvent msg) throws Exception {
        switch (msg.getType()) {
            case WELCOME:
                if (msg.getText().equals(PROTOCOL_VERSION)) {
                    logger.debug("protocol versions match");
                } else {
                    logger.warn("different protocol version");
                }
                ctx.writeAndFlush(new GoodGameEvent(GoodGameEventType.JOIN, channelId, null, null));
                break;
            case SUCCESS_JOIN:
                logger.debug("successfully joined channel {}", channelId);
                break;
            case MESSAGE:
                ctx.fireChannelRead(msg);
                break;
            case USER_BAN:
                ctx.fireChannelRead(msg);
                break;
            case ERROR:
                logger.debug("error {}", msg.getText());
                break;
        }
    }
}
