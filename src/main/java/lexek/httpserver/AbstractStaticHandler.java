package lexek.httpserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lexek.wschat.util.Tuple2;

import java.util.*;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.rtsp.RtspHeaderNames.CACHE_CONTROL;

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

    private final List<Tuple2<Pattern, Integer>> maxAgeOverrides = new ArrayList<>();

    public void addMaxAgeOverride(String path, int maxAge) {
        maxAgeOverrides.add(new Tuple2<>(Pattern.compile(path), maxAge));
    }

    @Override
    public FullHttpResponse handle(ViewResolvers viewResolvers, FullHttpRequest request, Channel channel) throws Exception {
        StaticHandlerContext context = getContext(withoutQuery(request.uri()));
        if (context.exists()) {
            Date lastModified = new Date(context.lastModified());

            FullHttpResponse response;
            if (checkIfNotModified(request, lastModified)) {
                response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED, Unpooled.EMPTY_BUFFER);
            } else {
                response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(context.read()));
            }
            HttpHeaders headers = response.headers();

            String contentType = context.getContentType();
            if (contentType != null) {
                headers.add(CONTENT_TYPE, contentType);
            }
            headers.add(ETAG, lastModified.toString());

            int maxAge = 3600;
            for (Tuple2<Pattern, Integer> override : maxAgeOverrides) {
                if (override.getL().matcher(request.uri()).matches()) {
                    maxAge = override.getR();
                    break;
                }
            }
            headers.add(CACHE_CONTROL, HttpHeaderValues.MAX_AGE.concat("=" + maxAge));
            headers.add(LAST_MODIFIED, lastModified);
            return response;
        } else {
            return null;
        }
    }

    private boolean checkIfNotModified(FullHttpRequest request, Date lastModified) {
        if (request.headers().contains(IF_MODIFIED_SINCE)) {
            long ifModifiedSince = request.headers().getTimeMillis(IF_MODIFIED_SINCE);
            return ifModifiedSince / 1000 >= lastModified.getTime() / 1000;
        } else {
            return false;
        }
    }

    private String withoutQuery(String path) {
        int queryStart = path.indexOf('?');
        if (queryStart > -1) {
            return path.substring(0, queryStart);
        } else {
            return path;
        }
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

    protected abstract StaticHandlerContext getContext(String uri);
}
