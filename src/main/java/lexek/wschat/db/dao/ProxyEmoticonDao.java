package lexek.wschat.db.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.model.ProxyEmoticon;
import org.jooq.DSLContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.ProxyEmoticon.PROXY_EMOTICON;

@Service
public class ProxyEmoticonDao {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DSLContext ctx;

    @Inject
    public ProxyEmoticonDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public List<ProxyEmoticon> getEmoticons(String providerName) {
        return ctx
            .selectFrom(PROXY_EMOTICON)
            .where(PROXY_EMOTICON.PROVIDER.eq(providerName))
            .fetch()
            .stream()
            .map(r -> new ProxyEmoticon(
                r.getId(),
                r.getCode(),
                r.getFileName(),
                ImmutableMap.of()
            ))
            .collect(Collectors.toList());
    }

    public void saveEmoticon(String provider, ProxyEmoticon emoticon) {
        try {
            String serializedExtra = objectMapper.writeValueAsString(emoticon.getExtra());
            ctx
                .insertInto(PROXY_EMOTICON)
                .columns(
                    PROXY_EMOTICON.PROVIDER, PROXY_EMOTICON.CODE, PROXY_EMOTICON.FILE_NAME, PROXY_EMOTICON.EXTRA
                )
                .values(provider, emoticon.getCode(), emoticon.getFileName(), serializedExtra)
                .onDuplicateKeyUpdate()
                .set(PROXY_EMOTICON.FILE_NAME, emoticon.getFileName())
                .set(PROXY_EMOTICON.EXTRA, serializedExtra)
                .execute();
        } catch (Exception e) {
            throw new InternalErrorException(e);
        }
    }

    public void deleteAll(String providerName) {
        ctx
            .delete(PROXY_EMOTICON)
            .where(PROXY_EMOTICON.PROVIDER.eq(providerName))
            .execute();
    }
}
