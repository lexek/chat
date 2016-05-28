package lexek.wschat.db.dao;

import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.model.ProxyAuth;
import lexek.wschat.db.model.UserDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.ProxyAuth.PROXY_AUTH;
import static lexek.wschat.db.jooq.tables.User.USER;

public class ProxyAuthDao {
    private final DataSource dataSource;

    public ProxyAuthDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ProxyAuth createOrUpdate(ProxyAuth proxyAuth) {
        try (Connection connection = dataSource.getConnection()) {
            return DSL.using(connection).transactionResult(configuration -> {
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
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public void delete(long id, Long ownerId) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
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
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public void delete(long id) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            int rows = ctx
                .delete(PROXY_AUTH)
                .where(PROXY_AUTH.ID.equal(id))
                .execute();
            if (rows != 1) {
                throw new EntityNotFoundException();
            }
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public ProxyAuth get(long id) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
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
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public ProxyAuth get(String service, String externalId) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
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
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public List<ProxyAuth> getAll(UserDto owner) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
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
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public List<ProxyAuth> getAll(UserDto owner, Set<String> services) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
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
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public List<ProxyAuth> getAll(Set<String> services) {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
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
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public List<ProxyAuth> getAll() {
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
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
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public void updateToken(Long id, String newToken) {
        try (Connection connection = dataSource.getConnection()) {
            DSL
                .using(connection)
                .update(PROXY_AUTH)
                .set(PROXY_AUTH.KEY, newToken)
                .where(
                    PROXY_AUTH.ID.equal(id)
                )
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }
}
