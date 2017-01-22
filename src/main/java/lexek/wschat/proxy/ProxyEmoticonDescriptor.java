package lexek.wschat.proxy;

import java.util.Map;

public class ProxyEmoticonDescriptor {
    private final String code;
    private final String url;
    private final String fileName;
    private final Map<String, Object> extra;

    public ProxyEmoticonDescriptor(String code, String url, String fileName, Map<String, Object> extra) {
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

    public Map<String, Object> getExtra() {
        return extra;
    }
}
