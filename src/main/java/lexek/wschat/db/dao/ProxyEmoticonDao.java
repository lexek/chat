package lexek.wschat.db.dao;

import lexek.wschat.db.model.ProxyEmoticon;
import org.jooq.DSLContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

import static lexek.wschat.db.jooq.tables.ProxyEmoticon.PROXY_EMOTICON;

@Service
public class ProxyEmoticonDao {
    private final DSLContext ctx;

    @Inject
    public ProxyEmoticonDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public List<ProxyEmoticon> getEmoticons(String service) {
        return null;
    }

    public void saveEmoticon(String provider, ProxyEmoticon emoticon) {
        ctx
            .insertInto(PROXY_EMOTICON)
            .columns(
                PROXY_EMOTICON.PROVIDER, PROXY_EMOTICON.CODE, PROXY_EMOTICON.FILE_NAME, PROXY_EMOTICON.EXTRA,
                PROXY_EMOTICON.WIDTH, PROXY_EMOTICON.HEIGHT
            )
            .values(provider, emoticon.getCode(), emoticon.getFileName(), null, emoticon.getWidth(), emoticon.getHeight())
            .execute();
    }
}