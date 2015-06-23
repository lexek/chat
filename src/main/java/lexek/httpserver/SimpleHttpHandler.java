package lexek.httpserver;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import lexek.wschat.util.Net;

public abstract class SimpleHttpHandler implements HttpHandler {
    @Override
    public FullHttpResponse handle(ViewResolvers viewResolvers, FullHttpRequest request, Channel channel) throws Exception {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            channel.alloc().buffer()
        );
        Request requestWrapper = new Request(Net.getIp(channel.remoteAddress()), request);
        Response responseWrapper = new Response(response, viewResolvers);

        Exception rethrow = null;
        try {
            handle(requestWrapper, responseWrapper);
        } catch (Exception e) {
            //release response since we aren't gonna use it now
            response.release();
            rethrow = e;
        } finally {
            requestWrapper.releaseResources();
        }
        if (rethrow != null) {
            throw rethrow;
        }
        return response;
    }

    abstract protected void handle(Request request, Response response) throws Exception;
}
