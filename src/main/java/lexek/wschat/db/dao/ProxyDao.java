package lexek.wschat.db.dao;

import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.jooq.tables.pojos.ChatProxy;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static lexek.wschat.db.jooq.tables.ChatProxy.CHAT_PROXY;

@Service
public class ProxyDao {
    private final DataSource dataSource;

    @Inject
    public ProxyDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void store(ChatProxy chatProxy) {
        try (Connection connection = dataSource.getConnection()) {
            long id = DSL.using(connection).insertInto(CHAT_PROXY)
                .set(CHAT_PROXY.ROOM_ID, chatProxy.getRoomId())
                .set(CHAT_PROXY.PROVIDER_NAME, chatProxy.getProviderName())
                .set(CHAT_PROXY.AUTH_ID, chatProxy.getAuthId())
                .set(CHAT_PROXY.REMOTE_ROOM, chatProxy.getRemoteRoom())
                .set(CHAT_PROXY.ENABLE_OUTBOUND, chatProxy.getEnableOutbound())
                .returning(CHAT_PROXY.ID)
                .fetchOne()
                .getId();
            chatProxy.setId(id);
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public List<ChatProxy> getAll() {
        List<ChatProxy> result;
        try (Connection connection = dataSource.getConnection()) {
            result = DSL.using(connection).selectFrom(CHAT_PROXY).fetchInto(ChatProxy.class);
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
    }

    public void remove(long id) {
        try (Connection connection = dataSource.getConnection()) {
            boolean ok = DSL.using(connection).delete(CHAT_PROXY).where(CHAT_PROXY.ID.equal(id)).execute() == 1;
            if (!ok) {
                throw new EntityNotFoundException();
            }
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }
}
