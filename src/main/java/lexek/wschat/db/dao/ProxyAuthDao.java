package lexek.wschat.db.dao;

import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.db.model.ProxyAuth;
import lexek.wschat.db.model.UserDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.ProxyAuth.PROXY_AUTH;
import static lexek.wschat.db.jooq.tables.User.USER;

@Service
public class ProxyAuthDao {
    private final DSLContext ctx;

    @Inject
    public ProxyAuthDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public ProxyAuth createOrUpdate(ProxyAuth proxyAuth) {
        return ctx.transactionResult(configuration -> {
            DSLContext ctx = DSL.using(configuration);

            String service = proxyAuth.getService();
            String externalId = proxyAuth.getExternalId();
            UserDto owner = proxyAuth.getOwner();

            Long id = null;

            Record existingRecord = ctx
                .select(PROXY_AUTH.ID)
                .from(PROXY_AUTH)
                .where(
                    PROXY_AUTH.SERVICE.equal(service),
                    PROXY_AUTH.EXTERNAL_ID.equal(externalId)
                )
                .fetchOne();

            if (existingRecord != null) {
                id = existingRecord.getValue(PROXY_AUTH.ID);
                ctx
                    .update(PROXY_AUTH)
                    .set(PROXY_AUTH.KEY, proxyAuth.getKey())
                    .set(PROXY_AUTH.OWNER_ID, owner.getId())
                    .set(PROXY_AUTH.EXTERNAL_NAME, proxyAuth.getExternalName())
                    .where(PROXY_AUTH.ID.equal(id))
                    .execute();
            } else {
                id = ctx
                    .insertInto(
                        PROXY_AUTH,
                        PROXY_AUTH.SERVICE,
                        PROXY_AUTH.EXTERNAL_ID,
                        PROXY_AUTH.EXTERNAL_NAME,
                        PROXY_AUTH.OWNER_ID,
                        PROXY_AUTH.KEY
                    )
                    .values(
                        proxyAuth.getService(),
                        proxyAuth.getExternalId(),
                        proxyAuth.getExternalName(),
                        proxyAuth.getOwner().getId(),
                        proxyAuth.getKey()
                    )
                    .returning(PROXY_AUTH.ID)
                    .fetchOne()
                    .value1();
            }
            return new ProxyAuth(id, proxyAuth);
        });
    }

    public void delete(long id, Long ownerId) {
        int rows = ctx
            .delete(PROXY_AUTH)
            .where(
                PROXY_AUTH.ID.equal(id),
                PROXY_AUTH.OWNER_ID.equal(ownerId)
            )
            .execute();
        if (rows != 1) {
            throw new EntityNotFoundException();
        }
    }

    public void delete(long id) {
        int rows = ctx
            .delete(PROXY_AUTH)
            .where(PROXY_AUTH.ID.equal(id))
            .execute();
        if (rows != 1) {
            throw new EntityNotFoundException();
        }
    }

    public ProxyAuth get(long id) {
        Record record = ctx
            .select()
            .from(PROXY_AUTH)
            .join(USER).on(USER.ID.equal(PROXY_AUTH.OWNER_ID))
            .where(PROXY_AUTH.ID.equal(id))
            .fetchOne();
        Record authRecord = record.into(PROXY_AUTH);
        return new ProxyAuth(
            authRecord.getValue(PROXY_AUTH.ID),
            authRecord.getValue(PROXY_AUTH.SERVICE),
            authRecord.getValue(PROXY_AUTH.EXTERNAL_ID),
            authRecord.getValue(PROXY_AUTH.EXTERNAL_NAME),
            UserDto.fromRecord(record.into(USER)),
            authRecord.getValue(PROXY_AUTH.KEY)
        );
    }

    public ProxyAuth get(String service, String externalId) {
        Record record = ctx
            .select()
            .from(PROXY_AUTH)
            .join(USER).on(USER.ID.equal(PROXY_AUTH.OWNER_ID))
            .where(
                PROXY_AUTH.SERVICE.equal(service),
                PROXY_AUTH.EXTERNAL_ID.equal(externalId)
            )
            .fetchOne();
        Record authRecord = record.into(PROXY_AUTH);
        return new ProxyAuth(
            authRecord.getValue(PROXY_AUTH.ID),
            authRecord.getValue(PROXY_AUTH.SERVICE),
            authRecord.getValue(PROXY_AUTH.EXTERNAL_ID),
            authRecord.getValue(PROXY_AUTH.EXTERNAL_NAME),
            UserDto.fromRecord(record.into(USER)),
            authRecord.getValue(PROXY_AUTH.KEY)
        );
    }

    public List<ProxyAuth> getAll(UserDto owner) {
        return ctx
            .select()
            .from(PROXY_AUTH)
            .join(USER).on(USER.ID.equal(PROXY_AUTH.OWNER_ID))
            .where(PROXY_AUTH.OWNER_ID.equal(owner.getId()))
            .fetch()
            .stream()
            .map(record -> {
                Record authRecord = record.into(PROXY_AUTH);
                return new ProxyAuth(
                    authRecord.getValue(PROXY_AUTH.ID),
                    authRecord.getValue(PROXY_AUTH.SERVICE),
                    authRecord.getValue(PROXY_AUTH.EXTERNAL_ID),
                    authRecord.getValue(PROXY_AUTH.EXTERNAL_NAME),
                    UserDto.fromRecord(record.into(USER)),
                    authRecord.getValue(PROXY_AUTH.KEY)
                );
            })
            .collect(Collectors.toList());
    }

    public List<ProxyAuth> getAll(UserDto owner, Set<String> services) {
        return ctx
            .select()
            .from(PROXY_AUTH)
            .join(USER).on(USER.ID.equal(PROXY_AUTH.OWNER_ID))
            .where(
                PROXY_AUTH.OWNER_ID.equal(owner.getId()),
                PROXY_AUTH.SERVICE.in(services)
            )
            .fetch()
            .stream()
            .map(record -> {
                Record authRecord = record.into(PROXY_AUTH);
                return new ProxyAuth(
                    authRecord.getValue(PROXY_AUTH.ID),
                    authRecord.getValue(PROXY_AUTH.SERVICE),
                    authRecord.getValue(PROXY_AUTH.EXTERNAL_ID),
                    authRecord.getValue(PROXY_AUTH.EXTERNAL_NAME),
                    UserDto.fromRecord(record.into(USER)),
                    authRecord.getValue(PROXY_AUTH.KEY)
                );
            })
            .collect(Collectors.toList());
    }

    public List<ProxyAuth> getAll(Set<String> services) {
        return ctx
            .select()
            .from(PROXY_AUTH)
            .join(USER).on(USER.ID.equal(PROXY_AUTH.OWNER_ID))
            .where(PROXY_AUTH.SERVICE.in(services))
            .fetch()
            .stream()
            .map(record -> {
                Record authRecord = record.into(PROXY_AUTH);
                return new ProxyAuth(
                    authRecord.getValue(PROXY_AUTH.ID),
                    authRecord.getValue(PROXY_AUTH.SERVICE),
                    authRecord.getValue(PROXY_AUTH.EXTERNAL_ID),
                    authRecord.getValue(PROXY_AUTH.EXTERNAL_NAME),
                    UserDto.fromRecord(record.into(USER)),
                    authRecord.getValue(PROXY_AUTH.KEY)
                );
            })
            .collect(Collectors.toList());
    }

    public List<ProxyAuth> getAll() {
        return ctx
            .select()
            .from(PROXY_AUTH)
            .join(USER).on(USER.ID.equal(PROXY_AUTH.OWNER_ID))
            .fetch()
            .stream()
            .map(record -> {
                Record authRecord = record.into(PROXY_AUTH);
                return new ProxyAuth(
                    authRecord.getValue(PROXY_AUTH.ID),
                    authRecord.getValue(PROXY_AUTH.SERVICE),
                    authRecord.getValue(PROXY_AUTH.EXTERNAL_ID),
                    authRecord.getValue(PROXY_AUTH.EXTERNAL_NAME),
                    UserDto.fromRecord(record.into(USER)),
                    authRecord.getValue(PROXY_AUTH.KEY)
                );
            })
            .collect(Collectors.toList());
    }

    public void updateToken(Long id, String newToken) {
        ctx
            .update(PROXY_AUTH)
            .set(PROXY_AUTH.KEY, newToken)
            .where(
                PROXY_AUTH.ID.equal(id)
            )
            .execute();
    }
}
