package lexek.httpserver;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public interface HttpHandler {
    FullHttpResponse handle(ViewResolvers viewResolvers, FullHttpRequest request, Channel channel) throws Exception;
}
