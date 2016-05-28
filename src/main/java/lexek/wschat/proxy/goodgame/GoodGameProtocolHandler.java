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
    private final CredentialsProvider credentialsProvider;
    private String lastUser = null;

    public GoodGameProtocolHandler(String channelId, CredentialsProvider credentialsProvider) {
        this.channelId = channelId;
        this.credentialsProvider = credentialsProvider;
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
                String password = null;
                String name = null;
                if (credentialsProvider != null) {
                    Credentials credentials = credentialsProvider.getCredentials();
                    name = credentials.getUserId();
                    password = credentials.getToken();
                }
                lastUser = name;
                ctx.writeAndFlush(new GoodGameEvent(GoodGameEventType.AUTH, null, null, password, name, null));
                break;
            case SUCCESS_AUTH:
                if (lastUser == null || msg.getUser().equals(lastUser)) {
                    ctx.writeAndFlush(new GoodGameEvent(GoodGameEventType.JOIN, channelId, null, null, null, null));
                } else {
                    ctx.fireChannelRead(new GoodGameEvent(GoodGameEventType.FAILED_AUTH, null, null, null, null, null));
                }
                break;
            case SUCCESS_JOIN:
                ctx.fireChannelRead(msg);
                break;
            case FAILED_JOIN:
            case BAD_RIGHTS:
            case MESSAGE:
            case USER_BAN:
                ctx.fireChannelRead(msg);
                break;
            case ERROR:
                logger.debug("error {}", msg.getText());
                break;
            default:
                logger.debug("unsupported message type {}", msg.getType());
                break;
        }
    }
}
