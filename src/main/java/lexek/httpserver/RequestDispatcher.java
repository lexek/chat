package lexek.httpserver;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import lexek.wschat.security.AuthenticationManager;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@ChannelHandler.Sharable
public class RequestDispatcher extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Set<String> csrfTypes = ImmutableSet.of(
        ContentType.TEXT_PLAIN.getMimeType(),
        ContentType.MULTIPART_FORM_DATA.getMimeType(),
        ContentType.APPLICATION_FORM_URLENCODED.getMimeType()
    );

    private final Logger slowRequestLogger = LoggerFactory.getLogger("slow-http");
    private final Logger logger = LoggerFactory.getLogger(RequestDispatcher.class);
    private final List<MatcherEntry> matcherEntries = new ArrayList<>();
    private final ServerMessageHandler serverMessageHandler;
    private final ViewResolvers viewResolvers;
    private final Timer timer;
    private final String host;
    private final String hostname;
    private final String origin;

    public RequestDispatcher(
        ServerMessageHandler serverMessageHandler,
        ViewResolvers viewResolvers,
        AuthenticationManager authenticationManager,
        Application resourceConfig,
        MetricRegistry metricRegistry,
        String hostname
    ) {
        this.serverMessageHandler = serverMessageHandler;
        this.viewResolvers = viewResolvers;
        this.host = hostname + ":1337";
        this.hostname = hostname;
        this.origin = "https://" + this.host;
        JerseyContainer jerseyContainer = new JerseyContainer(authenticationManager, resourceConfig);
        this.matcherEntries.add(new MatcherEntry(Pattern.compile("/rest/.*"), jerseyContainer));
        this.timer = metricRegistry.register("httpServer.requests", new Timer());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        Channel channel = ctx.channel();
        channel.config().setAutoRead(false);
        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        boolean hostnameOk = false;
        String host = request.headers().get(HttpHeaders.Names.HOST);
        String origin = request.headers().get(HttpHeaders.Names.ORIGIN);
        if (this.host.equalsIgnoreCase(host) && (origin == null || this.origin.equals(origin))) {
            hostnameOk = true;
        }

        boolean referrerOk = checkReferrer(request);

        FullHttpResponse response = null;
        if (hostnameOk && referrerOk) {
            Timer.Context timerContext = timer.time();
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
            long millis = TimeUnit.NANOSECONDS.toMillis(timerContext.stop());
            //slow request
            if (millis > 500) {
                slowRequestLogger.info(
                    "{} {} ({}) {} ms",
                    request.getUri(),
                    request.getMethod(),
                    response != null ? response.getStatus() : 0,
                    millis
                );
            }
        } else {
            if (!hostnameOk) {
                response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.FORBIDDEN,
                    channel.alloc().buffer());
                Response wrapper = new Response(response, viewResolvers);
                serverMessageHandler.handle(
                    new ServerMessage(HttpResponseStatus.FORBIDDEN, "Hostname verification failed."),
                    wrapper
                );
            } else {
                response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.FORBIDDEN,
                    channel.alloc().buffer());
                Response wrapper = new Response(response, viewResolvers);
                serverMessageHandler.handle(
                    new ServerMessage(HttpResponseStatus.FORBIDDEN, "Referrer check failed."),
                    wrapper
                );
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

    private boolean checkReferrer(HttpRequest request) {
        if (request.getMethod() == HttpMethod.POST) {
            String disableCheckHeader = request.headers().get("X-Referrer-Check");
            if (disableCheckHeader != null && disableCheckHeader.equals("no-check")) {
                return true;
            }
            String contentTypeHeader = request.headers().get(HttpHeaders.Names.CONTENT_TYPE);
            if (contentTypeHeader != null) {
                ContentType contentType = ContentType.parse(contentTypeHeader);
                if (!csrfTypes.contains(contentType.getMimeType())) {
                    return true;
                }
            }

            String referrer = request.headers().get(HttpHeaders.Names.REFERER);
            if (referrer == null) {
                return false;
            }
            try {
                URL url = new URL(referrer.toLowerCase());
                return (url.getHost().equals(hostname) && url.getPort() == 1337);
            } catch (MalformedURLException e) {
                return false;
            }
        } else {
            return true;
        }
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
            return path.substring(0, queryStart);
        } else {
            return path;
        }
    }
}
