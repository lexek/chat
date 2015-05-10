package lexek.wschat.db;

import lexek.wschat.db.jooq.tables.records.UserRecord;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.TableField;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.User.USER;
import static lexek.wschat.db.jooq.tables.Userauth.USERAUTH;

public class UserDao {
    private final DataSource dataSource;
    private final Logger logger = LoggerFactory.getLogger(UserDao.class);

    public UserDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean tryChangeName(long id, String newName, boolean ignoreCheck) {
        boolean result = false;
        try (Connection connection = dataSource.getConnection()) {
            Condition condition = USER.ID.equal(id);
            if (!ignoreCheck) {
                condition.and(USER.RENAMEAVAILABLE.equal(true));
            }
            result = DSL.using(connection)
                    .update(USER)
                    .set(USER.NAME, newName)
                    .set(USER.RENAMEAVAILABLE, false)
                    .where(condition)
                    .execute() == 1;
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public UserDto getByName(String name) {
        UserDto userDto = null;
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                    .select()
                    .from(USER)
                    .where(USER.NAME.equal(name))
                    .fetchOne();
            userDto = UserDto.fromRecord(record);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return userDto;
    }

    public UserDto getById(long id) {
        UserDto userDto = null;
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                    .select()
                    .from(USER)
                    .where(USER.ID.equal(id))
                    .fetchOne();
            userDto = UserDto.fromRecord(record);
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return userDto;
    }

    public boolean setFields(Map<TableField<UserRecord, ?>, Object> values, long id) {
        boolean success = false;
        try (Connection connection = dataSource.getConnection()) {
            success = DSL.using(connection)
                    .update(USER)
                    .set(values)
                    .where(USER.ID.equal(id))
                    .execute() == 1;
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return success;
    }

    public void setColor(long id, String color) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                    .update(USER)
                    .set(USER.COLOR, color)
                    .where(USER.ID.equal(id))
                    .execute();
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
    }

    public DataPage<UserData> getAllPaged(int page, int pageLength, SortField<?> sortField) {
        DataPage<UserData> result = null;
        try (Connection connection = dataSource.getConnection()) {
            List<UserData> data = DSL.using(connection)
                    .select(USER.ID, USER.NAME, USER.ROLE, USER.COLOR, USER.BANNED, USER.RENAMEAVAILABLE, USER.EMAIL,
                            DSL.groupConcat(USERAUTH.SERVICE).as("authServices"),
                            DSL.groupConcat(DSL.coalesce(USERAUTH.AUTHNAME, "")).as("authNames"))
                    .from(USER.join(USERAUTH).on(USER.ID.equal(USERAUTH.USER_ID)))
                    .groupBy(USER.ID)
                    .orderBy(sortField)
                    .limit(page * pageLength, pageLength)
                    .fetch()
                    .stream()
                    .map(record -> new UserData(
                            UserDto.fromRecord(record),
                            record.getValue("authServices", String.class),
                            record.getValue("authNames", String.class)
                    ))
                    .collect(Collectors.toList());
            result = new DataPage<>(data, page, totalPages(connection, null, pageLength));
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public DataPage<UserData> searchPaged(Integer page, int pageLength, SortField<?> sortField, String nameParam) {
        DataPage<UserData> result = null;
        try (Connection connection = dataSource.getConnection()) {
            List<UserData> data = DSL.using(connection)
                    .select(USER.ID, USER.NAME, USER.ROLE, USER.COLOR, USER.BANNED,
                            USER.RENAMEAVAILABLE, USER.EMAIL,
                            DSL.groupConcat(USERAUTH.SERVICE).as("authServices"),
                            DSL.groupConcat(DSL.coalesce(USERAUTH.AUTHNAME, "")).as("authNames"))
                    .from(USER.join(USERAUTH).on(USER.ID.equal(USERAUTH.USER_ID)))
                    .where(USER.NAME.like(nameParam, '!'))
                    .orderBy(sortField)
                    .limit(page * pageLength, pageLength)
                    .fetch()
                    .stream()
                    .map(record -> new UserData(
                            UserDto.fromRecord(record),
                            record.getValue("authServices", String.class),
                            record.getValue("authNames", String.class)
                    ))
                    .collect(Collectors.toList());
            result = new DataPage<>(data, page, totalPages(connection, USER.NAME.like(nameParam, '!'), pageLength));
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    private int totalPages(Connection connection, Condition condition, int pageLength) {
        if (condition != null) {
            return DSL.using(connection).fetchCount(DSL.select().from(USER).where(condition)) / pageLength;
        } else {
            return DSL.using(connection).fetchCount(DSL.select().from(USER)) / pageLength;
        }
    }

    public boolean delete(UserDto user) {
        boolean result = false;
        try (Connection connection = dataSource.getConnection()) {
            result = DSL.using(connection)
                    .delete(USER)
                    .where(USER.ID.equal(user.getId()))
                    .execute() == 1;
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public boolean checkName(String username) {
        boolean result = false;
        try (Connection connection = dataSource.getConnection()) {
            result = DSL.using(connection)
                    .selectOne()
                    .from(USER)
                    .where(USER.NAME.equal(username))
                    .fetchOne() == null;
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }

    public UserData fetchData(long id) {
        UserData result = null;
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                    .select(USER.ID, USER.NAME, USER.ROLE, USER.COLOR, USER.BANNED, USER.RENAMEAVAILABLE, USER.EMAIL,
                            DSL.groupConcat(USERAUTH.SERVICE).as("authServices"),
                            DSL.groupConcat(DSL.coalesce(USERAUTH.AUTHNAME, "")).as("authNames"))
                    .from(USER.join(USERAUTH).on(USER.ID.equal(USERAUTH.USER_ID)))
                    .where(USER.ID.equal(id))
                    .groupBy(USER.ID)
                    .fetchOne();
            if (record != null) {
                result = new UserData(
                        UserDto.fromRecord(record),
                        record.getValue("authServices", String.class),
                        record.getValue("authNames", String.class)
                );
            }
        } catch (DataAccessException | SQLException e) {
            logger.error("sql exception", e);
        }
        return result;
    }
}
