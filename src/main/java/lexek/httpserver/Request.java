package lexek.httpserver;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.AsciiString;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Request {
    private final String ip;
    private final FullHttpRequest nettyRequest;
    private HttpPostRequestDecoder postRequestDecoder;
    private QueryStringDecoder queryDecoder;
    private Map<String, String> cookies;

    public Request(String ip, FullHttpRequest nettyRequest) {
        this.ip = ip;
        this.nettyRequest = nettyRequest;
    }

    ByteBuf content() {
        return nettyRequest.content();
    }

    public HttpHeaders headers() {
        return nettyRequest.headers();
    }

    public String header(AsciiString key) {
        return nettyRequest.headers().get(key);
    }

    public boolean hasHeader(AsciiString key) {
        return nettyRequest.headers().contains(key);
    }

    private Map<String, String> getCookies() {
        if (this.cookies == null && hasHeader(HttpHeaderNames.COOKIE)) {
            ImmutableMap.Builder<String, String> mapBuilder = new ImmutableMap.Builder<>();
            for (Cookie cookie : ServerCookieDecoder.STRICT.decode(header(HttpHeaderNames.COOKIE))) {
                mapBuilder.put(cookie.name(), cookie.value());
            }
            this.cookies = mapBuilder.build();
        }
        return this.cookies;
    }

    public String cookieValue(String key) {
        if (getCookies() != null) {
            return getCookies().get(key);
        } else {
            return null;
        }
    }

    private HttpPostRequestDecoder getPostRequestDecoder() {
        if (this.postRequestDecoder == null) {
            this.postRequestDecoder = new HttpPostRequestDecoder(this.nettyRequest);
        }
        return this.postRequestDecoder;
    }

    private QueryStringDecoder getQueryDecoder() {
        if (this.queryDecoder == null) {
            this.queryDecoder = new QueryStringDecoder(nettyRequest.uri());
        }
        return this.queryDecoder;
    }

    public String postParam(String key) {
        HttpPostRequestDecoder decoder = getPostRequestDecoder();
        InterfaceHttpData data = decoder.getBodyHttpData(key);
        if (data != null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute && data instanceof Attribute) {
            try {
                return ((Attribute) data).getValue();
            } catch (IOException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public String queryParam(String key) {
        List<String> params = getQueryDecoder().parameters().get(key);
        return params != null ? params.get(0) : null;
    }

    public boolean queryParamAsBoolean(String key) {
        return "true".equalsIgnoreCase(queryParam(key));
    }

    public HttpMethod method() {
        return nettyRequest.method();
    }

    public String ip() {
        return ip;
    }

    public String uri() {
        return nettyRequest.uri();
    }

    void releaseResources() {
        if (this.postRequestDecoder != null) {
            this.postRequestDecoder.destroy();
        }
    }
}
