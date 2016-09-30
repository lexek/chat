package lexek.wschat.db.dao;

import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.db.jooq.tables.pojos.ChatProxy;
import org.jooq.DSLContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

import static lexek.wschat.db.jooq.tables.ChatProxy.CHAT_PROXY;

@Service
public class ProxyDao {
    private final DSLContext ctx;

    @Inject
    public ProxyDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public void store(ChatProxy chatProxy) {
        long id = ctx.insertInto(CHAT_PROXY)
            .set(CHAT_PROXY.ROOM_ID, chatProxy.getRoomId())
            .set(CHAT_PROXY.PROVIDER_NAME, chatProxy.getProviderName())
            .set(CHAT_PROXY.AUTH_ID, chatProxy.getAuthId())
            .set(CHAT_PROXY.REMOTE_ROOM, chatProxy.getRemoteRoom())
            .set(CHAT_PROXY.ENABLE_OUTBOUND, chatProxy.getEnableOutbound())
            .returning(CHAT_PROXY.ID)
            .fetchOne()
            .getId();
        chatProxy.setId(id);
    }

    public List<ChatProxy> getAll() {
        return ctx.selectFrom(CHAT_PROXY).fetchInto(ChatProxy.class);
    }

    public void remove(long id) {
        boolean ok = ctx.delete(CHAT_PROXY).where(CHAT_PROXY.ID.equal(id)).execute() == 1;
        if (!ok) {
            throw new EntityNotFoundException();
        }
    }
}
