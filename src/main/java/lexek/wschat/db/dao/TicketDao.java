package lexek.wschat.db.dao;

import lexek.wschat.db.jooq.tables.pojos.Ticket;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.rest.TicketRestModel;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.Ticket.TICKET;
import static lexek.wschat.db.jooq.tables.User.USER;

@Service
public class TicketDao {
    private final DSLContext ctx;

    @Inject
    public TicketDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public boolean add(final Ticket pojo) {
        int count = ctx.fetchCount(
            TICKET,
            DSL.and(
                TICKET.USER.equal(pojo.getUser()),
                TICKET.IS_OPEN.isTrue()
            )
        );
        if (count < 5) {
            ctx.executeInsert(ctx.newRecord(TICKET, pojo));
            return true;
        } else {
            return false;
        }
    }

    public Ticket getById(long id) {
        return ctx
            .select()
            .from(TICKET)
            .where(TICKET.ID.equal(id))
            .fetchOneInto(Ticket.class);
    }

    public void update(Ticket ticket) {
        ctx.newRecord(TICKET, ticket).update();
    }

    public DataPage<TicketRestModel> getAll(boolean isOpen, int page, int pageLength) {
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
        int total = ctx.fetchCount(
            TICKET,
            TICKET.IS_OPEN.equal(isOpen)
        );
        return new DataPage<>(data, page, total / pageLength);
    }

    public DataPage<Ticket> getAll(UserDto user, int page, int pageLength) {
        List<Ticket> data = ctx
            .select(TICKET.IS_OPEN, TICKET.CATEGORY, TICKET.TEXT, TICKET.ADMIN_REPLY)
            .from(TICKET)
            .where(TICKET.USER.equal(user.getId()))
            .orderBy(TICKET.TIMESTAMP.desc())
            .limit(page * pageLength, pageLength)
            .fetchInto(Ticket.class);
        int total = ctx.fetchCount(
            TICKET,
            TICKET.USER.equal(user.getId())
        ) / pageLength;
        return new DataPage<>(data, page, total / pageLength);
    }

    public Ticket close(long id, long closedBy, String comment) {
        Ticket ticket;
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
            .fetchOne()
            .into(Ticket.class);
        return ticket;
    }

    public int getCount() {
        return ctx.fetchCount(DSL.select().from(TICKET).where(TICKET.IS_OPEN.isTrue()));
    }

}
