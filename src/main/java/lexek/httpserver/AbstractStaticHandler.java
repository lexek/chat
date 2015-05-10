package lexek.httpserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.CharsetUtil;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class AbstractStaticHandler implements HttpHandler {
    public static final Map<String, String> DEFAULT_MIME_TYPES;

    static {
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("csv", "text/csv");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("xml", "text/xml");
        mimeTypes.put("js", "text/javascript");
        mimeTypes.put("xhtml", "application/xhtml+xml");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("zip", "application/zip");
        mimeTypes.put("tar", "application/x-tar");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("tiff", "image/tiff");
        mimeTypes.put("tif", "image/tiff");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("swf", "application/x-shockwave-flash");
        mimeTypes.put("svg", "image/svg+xml");
        mimeTypes.put("ico", "image/vnd.microsoft.icon");
        DEFAULT_MIME_TYPES = Collections.unmodifiableMap(mimeTypes);
    }

    @Override
    public FullHttpResponse handle(ViewResolvers viewResolvers, FullHttpRequest request, Channel channel) throws Exception {
        StaticHandlerContext context = getContext(withoutQuery(request.getUri()));
        if (context.exists()) {
            Date lastModified = new Date(context.lastModified());

            FullHttpResponse response;
            if (checkIfNotModified(request, lastModified)) {
                response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED, Unpooled.EMPTY_BUFFER);
            } else {
                response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(context.read()));
            }

            String contentType = context.getContentType();
            if (contentType != null) {
                response.headers().add(CONTENT_TYPE, contentType);
            }
            response.headers().add(ETAG, lastModified.toString());
            response.headers().add(CACHE_CONTROL, HttpHeaders.Values.MAX_AGE + "=120");
            HttpHeaders.setDateHeader(response, LAST_MODIFIED, lastModified);
            return response;
        } else {
            return null;
        }
    }

    private boolean checkIfNotModified(FullHttpRequest request, Date lastModified) throws ParseException {
        if (request.headers().contains(IF_MODIFIED_SINCE)) {
            Date ifModifiedSince = HttpHeaders.getDateHeader(request, IF_MODIFIED_SINCE);
            return ifModifiedSince.getTime() / 1000 >= lastModified.getTime() / 1000;
        } else {
            return false;
        }
    }

    private String withoutQuery(String path) {
        int queryStart = path.indexOf('?');
        if (queryStart > -1) {
            path = path.substring(0, queryStart);
        }
        return path;
    }

    protected String guessMimeType(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot == -1) {
            return null;
        }
        String extension = path.substring(lastDot + 1).toLowerCase();
        String mimeType = DEFAULT_MIME_TYPES.get(extension);
        if (mimeType == null) {
            return null;
        }
        if (mimeType.startsWith("text/")) {
            mimeType += "; charset=" + CharsetUtil.UTF_8;
        }
        return mimeType;
    }

    abstract StaticHandlerContext getContext(String uri);
}
