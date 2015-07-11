package lexek.wschat.db.dao;

import lexek.wschat.db.jooq.tables.pojos.Ticket;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.rest.TicketRestModel;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.Ticket.TICKET;
import static lexek.wschat.db.jooq.tables.User.USER;

public class TicketDao {
    private final Logger logger = LoggerFactory.getLogger(TicketDao.class);
    private final DataSource dataSource;

    public TicketDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean add(final Ticket pojo) {
        boolean success = false;
        try (Connection connection = dataSource.getConnection()) {
            success = DSL.using(connection).transactionResult(c -> {
                int count = DSL.using(c).fetchCount(
                    DSL.using(c)
                        .select()
                        .from(TICKET)
                        .where(TICKET.USER.equal(pojo.getUser()).and(TICKET.IS_OPEN.isTrue())));
                if (count < 5) {
                    DSL.using(c).executeInsert(DSL.using(c).newRecord(TICKET, pojo));
                    return true;
                } else {
                    return false;
                }
            });
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return success;
    }

    public Ticket getById(long id) {
        Ticket result = null;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            result = ctx
                .select()
                .from(TICKET)
                .where(TICKET.ID.equal(id))
                .fetchOneInto(Ticket.class);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public void update(Ticket ticket) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection).newRecord(TICKET, ticket).update();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }

    public DataPage<TicketRestModel> getAll(boolean isOpen, int page, int pageLength) {
        DataPage<TicketRestModel> result = null;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            List<TicketRestModel> data = ctx
                .select()
                .from(TICKET)
                .join(USER.as("user")).on(TICKET.USER.equal(USER.as("user").ID))
                .leftOuterJoin(USER.as("closedBy")).on(TICKET.CLOSED_BY.equal(USER.as("closedBy").ID))
                .where(TICKET.IS_OPEN.equal(isOpen))
                .orderBy(TICKET.TIMESTAMP.desc())
                .limit(page * pageLength, pageLength)
                .fetch()
                .stream()
                .map(record -> new TicketRestModel(
                    UserDto.fromRecord(record.into(USER.as("user"))),
                    record.into(TICKET).into(Ticket.class),
                    record.getValue(USER.as("closedBy").NAME)))
                .collect(Collectors.toList());
            int total = ctx.fetchCount(ctx.select().from(TICKET).where(TICKET.IS_OPEN.equal(isOpen))) / pageLength;
            result = new DataPage<>(data, page, total / pageLength);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public DataPage<Ticket> getAll(UserDto user, int page, int pageLength) {
        DataPage<Ticket> result = null;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            List<Ticket> data = ctx
                .select(TICKET.IS_OPEN, TICKET.CATEGORY, TICKET.TEXT, TICKET.ADMIN_REPLY)
                .from(TICKET)
                .where(TICKET.USER.equal(user.getId()))
                .orderBy(TICKET.TIMESTAMP.desc())
                .limit(page * pageLength, pageLength)
                .fetchInto(Ticket.class);
            int total = ctx.fetchCount(ctx.select().from(TICKET).where(TICKET.USER.equal(user.getId()))) / pageLength;
            result = new DataPage<>(data, page, total / pageLength);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public String getTicketsForUser(long userId) {
        String result = null;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            result = ctx
                .select()
                .from(TICKET)
                .where(TICKET.USER.equal(userId))
                .orderBy(TICKET.TIMESTAMP.desc())
                .limit(10)
                .fetch()
                .formatJSON();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public List<Ticket> getNotDeliveredTicketsForUser(long userId) {
        List<Ticket> result = null;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            result = ctx
                .select()
                .from(TICKET)
                .where(TICKET.USER.equal(userId)
                    .and(TICKET.REPLY_DELIVERED.isFalse().or(TICKET.REPLY_DELIVERED.isNull()))
                    .and(TICKET.IS_OPEN.isFalse()))
                .fetch()
                .into(Ticket.class);
            ctx
                .update(TICKET)
                .set(TICKET.REPLY_DELIVERED, true)
                .where(TICKET.USER.equal(userId)
                    .and(TICKET.REPLY_DELIVERED.isFalse().or(TICKET.REPLY_DELIVERED.isNull()))
                    .and(TICKET.IS_OPEN.isFalse()))
                .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public Ticket close(long id, long closedBy, String comment) {
        Ticket ticket = null;
        try (Connection connection = dataSource.getConnection()) {
            DSLContext ctx = DSL.using(connection);
            ctx
                .update(TICKET)
                .set(TICKET.ADMIN_REPLY, comment)
                .set(TICKET.IS_OPEN, false)
                .set(TICKET.CLOSED_BY, closedBy)
                .where(TICKET.ID.equal(id).and(TICKET.IS_OPEN.isTrue()))
                .execute();
            ticket = ctx
                .select()
                .from(TICKET)
                .where(TICKET.ID.equal(id))
                .fetchOne().into(Ticket.class);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return ticket;
    }

    public int getCount() {
        int count = 0;
        try (Connection connection = dataSource.getConnection()) {
            count = DSL.using(connection).fetchCount(DSL.select().from(TICKET).where(TICKET.IS_OPEN.isTrue()));
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return count;
    }

}
