package lexek.wschat.db.dao;

import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserData;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.UserChangeSet;
import lexek.wschat.util.Pages;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.User.USER;
import static lexek.wschat.db.jooq.tables.Userauth.USERAUTH;

@Service
public class UserDao {
    private final DataSource dataSource;

    @Inject
    public UserDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean tryChangeName(long id, String newName, boolean ignoreCheck) {
        try (Connection connection = dataSource.getConnection()) {
            Condition condition = USER.ID.equal(id);
            if (!ignoreCheck) {
                condition.and(USER.RENAME_AVAILABLE.equal(true));
            }
            return DSL.using(connection)
                .update(USER)
                .set(USER.NAME, newName)
                .set(USER.RENAME_AVAILABLE, false)
                .where(condition)
                .execute() == 1;
        } catch (DataAccessException e) {
            throw new InvalidInputException("name", "NAME_TAKEN");
        } catch (SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public UserDto getByName(String name) {
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                .select()
                .from(USER)
                .where(USER.NAME.equal(name))
                .fetchOne();
            return UserDto.fromRecord(record);
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public UserDto getById(long id) {
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                .select()
                .from(USER)
                .where(USER.ID.equal(id))
                .fetchOne();
            return UserDto.fromRecord(record);
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public UserDto update(long id, UserChangeSet changeSet) {
        Map<org.jooq.Field<?>, Object> changeMap = new HashMap<>();
        if (changeSet.getBanned() != null) {
            changeMap.put(USER.BANNED, changeSet.getBanned());
        }
        if (changeSet.getRenameAvailable() != null) {
            changeMap.put(USER.RENAME_AVAILABLE, changeSet.getRenameAvailable());
        }
        if (changeSet.getName() != null) {
            changeMap.put(USER.NAME, changeSet.getName());
        }
        if (changeSet.getRole() != null) {
            changeMap.put(USER.ROLE, changeSet.getRole().toString());
        }
        UserDto userDto = null;
        try (Connection connection = dataSource.getConnection()) {
            boolean success = DSL.using(connection)
                .update(USER)
                .set(changeMap)
                .where(USER.ID.equal(id))
                .execute() == 1;
            if (success) {
                Record record = DSL.using(connection)
                    .selectFrom(USER)
                    .where(USER.ID.equal(id))
                    .fetchOne();
                userDto = UserDto.fromRecord(record);
            }
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return userDto;
    }

    public void setColor(long id, String color) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .update(USER)
                .set(USER.COLOR, color)
                .where(USER.ID.equal(id))
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public DataPage<UserData> getAllPaged(int page, int pageLength) {
        try (Connection connection = dataSource.getConnection()) {
            List<UserData> data = DSL.using(connection)
                .select(USER.ID, USER.NAME, USER.ROLE, USER.COLOR, USER.BANNED, USER.RENAME_AVAILABLE, USER.EMAIL, USER.EMAIL_VERIFIED, USER.CHECK_IP,
                    DSL.groupConcat(USERAUTH.SERVICE).as("authServices"),
                    DSL.groupConcat(DSL.coalesce(USERAUTH.AUTH_NAME, "")).as("authNames"))
                .from(USER.join(USERAUTH).on(USER.ID.equal(USERAUTH.USER_ID)))
                .groupBy(USER.ID)
                .orderBy(USER.ID)
                .limit(page * pageLength, pageLength)
                .fetch()
                .stream()
                .map(record -> new UserData(
                    UserDto.fromRecord(record),
                    collectAuthServices(
                        record.getValue("authServices", String.class),
                        record.getValue("authNames", String.class)
                    )
                ))
                .collect(Collectors.toList());
            return new DataPage<>(data, page, Pages.pageCount(pageLength, DSL.using(connection).fetchCount(USER)));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public DataPage<UserData> searchPaged(Integer page, int pageLength, String nameParam) {
        try (Connection connection = dataSource.getConnection()) {
            List<UserData> data = DSL.using(connection)
                .select(USER.ID, USER.NAME, USER.ROLE, USER.COLOR, USER.BANNED, USER.CHECK_IP,
                    USER.RENAME_AVAILABLE, USER.EMAIL, USER.EMAIL_VERIFIED,
                    DSL.groupConcat(USERAUTH.SERVICE).as("authServices"),
                    DSL.groupConcat(DSL.coalesce(USERAUTH.AUTH_NAME, "")).as("authNames"))
                .from(USER.join(USERAUTH).on(USER.ID.equal(USERAUTH.USER_ID)))
                .where(USER.NAME.like(nameParam, '!'))
                .groupBy(USER.ID)
                .orderBy(USER.ID)
                .limit(page * pageLength, pageLength)
                .fetch()
                .stream()
                .map(record -> new UserData(
                    UserDto.fromRecord(record),
                    collectAuthServices(
                        record.getValue("authServices", String.class),
                        record.getValue("authNames", String.class)
                    )
                ))
                .collect(Collectors.toList());

            return new DataPage<>(data, page,
                Pages.pageCount(pageLength, DSL.using(connection).fetchCount(USER, USER.NAME.like(nameParam, '!'))));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public List<UserDto> searchSimple(int pageLength, String nameParam) {
        try (Connection connection = dataSource.getConnection()) {
            return DSL.using(connection)
                .selectFrom(USER)
                .where(USER.NAME.like(nameParam, '!'))
                .groupBy(USER.ID)
                .orderBy(USER.ID)
                .limit(pageLength)
                .fetch()
                .stream()
                .map(UserDto::fromRecord)
                .collect(Collectors.toList());
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public boolean delete(UserDto user) {
        try (Connection connection = dataSource.getConnection()) {
            return DSL.using(connection)
                .delete(USER)
                .where(USER.ID.equal(user.getId()))
                .execute() == 1;
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public boolean checkName(String username) {
        try (Connection connection = dataSource.getConnection()) {
            return DSL.using(connection)
                .selectOne()
                .from(USER)
                .where(USER.NAME.equal(username))
                .fetchOne() == null;
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public UserData fetchData(long id) {
        UserData result = null;
        try (Connection connection = dataSource.getConnection()) {
            Record record = DSL.using(connection)
                .select(USER.ID, USER.NAME, USER.ROLE, USER.COLOR, USER.BANNED, USER.RENAME_AVAILABLE, USER.EMAIL,
                    USER.EMAIL_VERIFIED, USER.CHECK_IP,
                    DSL.groupConcat(USERAUTH.SERVICE).as("authServices"),
                    DSL.groupConcat(DSL.coalesce(USERAUTH.AUTH_NAME, "")).as("authNames"))
                .from(USER.join(USERAUTH).on(USER.ID.equal(USERAUTH.USER_ID)))
                .where(USER.ID.equal(id))
                .groupBy(USER.ID)
                .fetchOne();
            if (record != null) {
                result = new UserData(
                    UserDto.fromRecord(record),
                    collectAuthServices(
                        record.getValue("authServices", String.class),
                        record.getValue("authNames", String.class)
                    )
                );
            }
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
    }

    public List<UserDto> getAdmins() {
        try (Connection connection = dataSource.getConnection()) {
            return DSL.using(connection)
                .select()
                .from(USER)
                .where(USER.ROLE.in("ADMIN", "SUPERADMIN"))
                .fetch()
                .stream()
                .map(UserDto::fromRecord)
                .collect(Collectors.toList());
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public void setCheckIp(UserDto user, boolean value) {
        try (Connection connection = dataSource.getConnection()) {
            int rows = DSL.using(connection)
                .update(USER)
                .set(USER.CHECK_IP, value)
                .where(USER.ID.equal(user.getId()))
                .execute();
            if (rows == 0) {
                throw new EntityNotFoundException("user");
            }
        } catch (SQLException e) {
            throw new InternalErrorException(e);
        }
    }


    private Map<String, String> collectAuthServices(String servicesString, String namesString) {
        String[] authServices = servicesString.split(",", -1);
        String[] authNames = namesString.split(",", -1);
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < authServices.length; ++i) {
            result.put(authServices[i], authNames[i]);
        }
        return result;
    }
}
