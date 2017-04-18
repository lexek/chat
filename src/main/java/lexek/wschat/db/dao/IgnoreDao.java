package lexek.wschat.db.dao;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.chat.model.User;
import org.jooq.Allow;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.IgnoreList.IGNORE_LIST;
import static lexek.wschat.db.jooq.tables.User.USER;

@Service
public class IgnoreDao {
    private final DSLContext ctx;

    @Inject
    public IgnoreDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public void addIgnore(User user, String name) {
        try {
            int result = ctx
                .insertInto(IGNORE_LIST, IGNORE_LIST.USER_ID, IGNORE_LIST.IGNORED_ID)
                .select(DSL.select(DSL.inline(user.getId()), USER.ID).from(USER).where(USER.NAME.equal(name)))
                .execute();
            if (result != 1) {
                throw new EntityNotFoundException("user");
            }
        } catch (DataAccessException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof MySQLIntegrityConstraintViolationException) {
                throw new InvalidInputException("name", "ALREADY_IGNORED");
            } else {
                throw e;
            }
        }
    }

    @Allow.PlainSQL
    public void deleteIgnore(User user, String name) {
        //todo: change to jooq query when multi table delete will be implemented in jooq
        String query =
            "delete ignore_list " +
                "from ignore_list join user as ignored on ignored.id = ignore_list.ignored_id " +
                "where ignore_list.user_id = ? and ignored.name = ?";
        int result = ctx.execute(query, user.getId(), name);
        if (result != 1) {
            throw new EntityNotFoundException("user");
        }
    }

    public List<String> fetchIgnoreList(User user) {
        return ctx
            .select(USER.NAME)
            .from(IGNORE_LIST.join(USER).on(IGNORE_LIST.IGNORED_ID.equal(USER.ID)))
            .where(IGNORE_LIST.USER_ID.equal(user.getId()))
            .fetch()
            .stream()
            .map(Record1::value1)
            .collect(Collectors.toList());
    }

    public int fetchIgnoreCount(User user) {
        return ctx.fetchCount(IGNORE_LIST, IGNORE_LIST.USER_ID.equal(user.getId()));
    }
}
