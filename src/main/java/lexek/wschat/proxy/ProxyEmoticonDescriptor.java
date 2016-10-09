package lexek.wschat.proxy;

import java.util.Map;

public class ProxyEmoticonDescriptor {
    private final String code;
    private final String url;
    private final String fileName;
    private final Map<Key, Object> extra;

    public ProxyEmoticonDescriptor(String code, String url, String fileName, Map<Key, Object> extra) {
        this.code = code;
        this.url = url;
        this.fileName = fileName;
        this.extra = extra;
    }

    public String getCode() {
        return code;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }

    public Map<Key, Object> getExtra() {
        return extra;
    }

    public <T> T extra(Key<T> key) {
        return (T) extra.get(key);
    }

    public class Key<T> {

    }
}
