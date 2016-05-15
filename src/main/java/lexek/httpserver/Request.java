package lexek.httpserver;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import lexek.wschat.db.model.SessionDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Request {
    private final String ip;
    private final FullHttpRequest nettyRequest;
    private HttpPostRequestDecoder postRequestDecoder;
    private QueryStringDecoder queryDecoder;
    private Map<String, String> cookies;
    private SessionDto session;

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

    public String header(String key) {
        return nettyRequest.headers().get(key);
    }

    public boolean hasHeader(String key) {
        return nettyRequest.headers().contains(key);
    }

    private Map<String, String> getCookies() {
        if (this.cookies == null && hasHeader(HttpHeaders.Names.COOKIE)) {
            ImmutableMap.Builder<String, String> mapBuilder = new ImmutableMap.Builder<>();
            for (Cookie cookie : ServerCookieDecoder.STRICT.decode(header(HttpHeaders.Names.COOKIE))) {
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
            this.queryDecoder = new QueryStringDecoder(nettyRequest.getUri());
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

    public Long postParamAsLong(String key) {
        String s = postParam(key);
        return s != null ? Longs.tryParse(s) : null;
    }

    public Integer postParamAsInteger(String key) {
        String s = postParam(key);
        return s != null ? Ints.tryParse(s) : null;
    }

    public boolean postParamAsBoolean(String key) {
        return "true".equalsIgnoreCase(postParam(key));
    }

    public List<String> postParams(String key) {
        HttpPostRequestDecoder decoder = getPostRequestDecoder();
        List<InterfaceHttpData> dataList = decoder.getBodyHttpDatas(key);
        List<String> result = new ArrayList<>();
        if (dataList != null) {
            result = dataList
                .stream()
                .filter(data -> data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute)
                .map(data -> {
                    try {
                        return ((Attribute) data).getValue();
                    } catch (IOException e) {
                        return null;
                    }
                })
                .filter(s -> s != null)
                .collect(Collectors.toList());
        }
        return result;
    }

    public FileUpload postParamFile(String key) {
        HttpPostRequestDecoder decoder = getPostRequestDecoder();
        InterfaceHttpData data = decoder.getBodyHttpData(key);
        if (data != null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
            return (FileUpload) data;
        } else {
            return null;
        }
    }

    public String queryParam(String key) {
        List<String> params = getQueryDecoder().parameters().get(key);
        return params != null ? params.get(0) : null;
    }

    public Long queryParamAsLong(String key) {
        String s = queryParam(key);
        return s != null ? Longs.tryParse(s) : null;
    }

    public Integer queryParamAsInteger(String key) {
        String s = queryParam(key);
        return s != null ? Ints.tryParse(s) : null;
    }

    public boolean queryParamAsBoolean(String key) {
        return "true".equalsIgnoreCase(queryParam(key));
    }

    public List<String> queryParams(String key) {
        return getQueryDecoder().parameters().get(key);
    }

    public Set<String> queryParamKeys() {
        return getQueryDecoder().parameters().keySet();
    }

    public HttpMethod method() {
        return nettyRequest.getMethod();
    }

    public String ip() {
        return ip;
    }

    public String uri() {
        return nettyRequest.getUri();
    }

    void releaseResources() {
        if (this.postRequestDecoder != null) {
            this.postRequestDecoder.destroy();
        }
    }
}
