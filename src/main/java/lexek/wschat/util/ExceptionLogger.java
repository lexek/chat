package lexek.wschat.util;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ChannelHandler.Sharable
public class ExceptionLogger extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(ExceptionLogger.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            logger.debug("io exception on {}", ctx.channel().remoteAddress(), cause);
        } else {
            logger.warn("exception on {}", ctx.channel().remoteAddress(), cause);
        }
        ReferenceCountUtil.release(cause);
    }
}
