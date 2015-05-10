package lexek.httpserver;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@ChannelHandler.Sharable
public class RequestDispatcher extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final Logger logger = LoggerFactory.getLogger(RequestDispatcher.class);
    private final List<MatcherEntry> matcherEntries = new ArrayList<>();
    private final ServerMessageHandler serverMessageHandler;
    private final ViewResolvers viewResolvers;

    public RequestDispatcher(ServerMessageHandler serverMessageHandler, ViewResolvers viewResolvers) {
        this.serverMessageHandler = serverMessageHandler;
        this.viewResolvers = viewResolvers;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        Channel channel = ctx.channel();
        channel.config().setAutoRead(false);
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        FullHttpResponse response = null;
        for (MatcherEntry matcherEntry : matcherEntries) {
            if (matcherEntry.getPattern().matcher(withoutQuery(request.getUri())).matches()) {
                try {
                    response = matcherEntry.getHandler().handle(viewResolvers, request, channel);
                } catch (Exception e) {
                    logger.warn("Exception while handling request", e);
                    response = createInternalErrorResponse(e, channel.alloc());
                }
                if (response != null) {
                    break;
                }
            }
        }

        if (response == null) {
            response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.NOT_FOUND,
                    channel.alloc().buffer());
            serverMessageHandler.handle(
                    new ServerMessage(HttpResponseStatus.NOT_FOUND, null),
                    new Response(response, viewResolvers));
        }

        HttpHeaders.setContentLength(response, response.content().readableBytes());
        HttpHeaders.setDate(response, new Date());
        response.headers().add(HttpHeaders.Names.SERVER, "Kappa server v.1.3.3.7");
        if (keepAlive) {
            response.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        } else {
            response.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        }
        ChannelFuture writeFuture = channel.writeAndFlush(response);
        if (!keepAlive) {
            writeFuture.addListener(ChannelFutureListener.CLOSE);
        }

        channel.config().setAutoRead(true);
    }

    private FullHttpResponse createInternalErrorResponse(Exception e, ByteBufAllocator allocator) throws Exception {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        e.printStackTrace(writer);
        writer.flush();
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                allocator.buffer());
        Response wrapper = new Response(response, viewResolvers);
        serverMessageHandler.handle(new ServerMessage(HttpResponseStatus.INTERNAL_SERVER_ERROR, buffer.toString()), wrapper);
        return response;
    }

    public void add(String pattern, HttpHandler handler) {
        matcherEntries.add(new MatcherEntry(Pattern.compile(pattern), handler));
    }

    private String withoutQuery(String path) {
        int queryStart = path.indexOf('?');
        if (queryStart > -1) {
            path = path.substring(0, queryStart);
        }
        return path;
    }
}
