package lexek.wschat.db.model;

import java.util.Map;

public class ProxyEmoticon extends Emoticon {
    private Map<String, Object> extra;

    public ProxyEmoticon() {
        super();
    }

    public ProxyEmoticon(Long id, String code, String fileName, Map<String, Object> extra) {
        super(id, code, fileName, null, null);
        this.extra = extra;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}
