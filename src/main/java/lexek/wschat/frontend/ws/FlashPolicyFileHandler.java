package lexek.wschat.frontend.ws;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

@ChannelHandler.Sharable
public class FlashPolicyFileHandler extends ChannelInboundHandlerAdapter {
    private static final ByteBuf FLASH_POLICY_REQUEST = Unpooled
        .copiedBuffer("<policy-file-request/>\0", CharsetUtil.US_ASCII);
    private final ByteBuf preparedResponse;

    public FlashPolicyFileHandler(int port) {
        this.preparedResponse = generateResponse(port);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object in) {
        if (in instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) in;
            // Will use the first 23 bytes to detect the policy file request.
            if (buf.readableBytes() >= 23) {
                ChannelPipeline p = ctx.pipeline();
                ByteBuf firstMessage = buf.readBytes(23);

                if (FLASH_POLICY_REQUEST.equals(firstMessage)) {
                    ctx.writeAndFlush(this.preparedResponse.retain().duplicate()).addListener(ChannelFutureListener.CLOSE);
                }

                p.remove(this);

                if (buf.readableBytes() > 0) {
                    ctx.fireChannelRead(firstMessage);
                    ctx.fireChannelRead(buf);
                } else {
                    ctx.fireChannelRead(firstMessage);
                    buf.release();
                }
            }
        }
    }

    private ByteBuf generateResponse(int port) {
        return Unpooled.copiedBuffer(
            "<?xml version=\"1.0\"?>\r\n"
                + "<!DOCTYPE cross-domain-policy SYSTEM \"/xml/dtds/cross-domain-policy.dtd\">\r\n"
                + "<cross-domain-policy>\r\n"
                + "  <site-control permitted-cross-domain-policies=\"master-only\"/>\r\n"
                + "  <allow-access-from domain=\"*\" to-ports=\"" + port + "\" />\r\n"
                + "</cross-domain-policy>\r\n",
            CharsetUtil.US_ASCII
        );
    }
}
