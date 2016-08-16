package lexek.wschat.db.dao;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.db.model.UserDto;
import org.jooq.Record1;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.IgnoreList.IGNORE_LIST;
import static lexek.wschat.db.jooq.tables.User.USER;

@Service
public class IgnoreDao {
    private final DataSource dataSource;

    @Inject
    public IgnoreDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void addIgnore(UserDto user, String name) {
        try (Connection connection = dataSource.getConnection()) {
            int result = DSL.using(connection)
                .insertInto(IGNORE_LIST, IGNORE_LIST.USER_ID, IGNORE_LIST.IGNORED_ID)
                .select(DSL.select(DSL.inline(user.getId()), USER.ID).from(USER).where(USER.NAME.equal(name)))
                .execute();
            if (result != 1) {
                throw new EntityNotFoundException("user");
            }
        } catch (SQLException e) {
            throw new InternalErrorException(e);
        } catch (DataAccessException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof MySQLIntegrityConstraintViolationException) {
                throw new InvalidInputException("name", "ALREADY_IGNORED");
            } else {
                throw e;
            }
        }
    }

    public void deleteIgnore(UserDto user, String name) {
        try (Connection connection = dataSource.getConnection()) {
            //todo: change to jooq query when multi table delete will be implemented in jooq
            String query =
                "delete ignore_list " +
                    "from ignore_list join user as ignored on ignored.id = ignore_list.ignored_id " +
                    "where ignore_list.user_id = ? and ignored.name = ?";
            int result = DSL.using(connection).execute(query, user.getId(), name);
            if (result != 1) {
                throw new EntityNotFoundException("user");
            }
        } catch (SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public List<String> fetchIgnoreList(UserDto user) {
        try (Connection connection = dataSource.getConnection()) {
            return DSL.using(connection)
                .select(USER.NAME)
                .from(IGNORE_LIST.join(USER).on(IGNORE_LIST.IGNORED_ID.equal(USER.ID)))
                .where(IGNORE_LIST.USER_ID.equal(user.getId()))
                .fetch()
                .stream()
                .map(Record1::value1)
                .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public int fetchIgnoreCount(UserDto user) {
        try (Connection connection = dataSource.getConnection()) {
            return DSL.using(connection).fetchCount(IGNORE_LIST, IGNORE_LIST.USER_ID.equal(user.getId()));
        } catch (SQLException e) {
            throw new InternalErrorException(e);
        }
    }
}
