package lexek.wschat.db.dao;

import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserData;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.UserChangeSet;
import lexek.wschat.util.Pages;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.User.USER;
import static lexek.wschat.db.jooq.tables.Userauth.USERAUTH;

@Service
public class UserDao {
    private final DSLContext ctx;

    @Inject
    public UserDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    public boolean tryChangeName(long id, String newName, boolean ignoreCheck) {
        try {
            Condition condition = USER.ID.equal(id);
            if (!ignoreCheck) {
                condition.and(USER.RENAME_AVAILABLE.equal(true));
            }
            return ctx
                .update(USER)
                .set(USER.NAME, newName)
                .set(USER.RENAME_AVAILABLE, false)
                .where(condition)
                .execute() == 1;
        } catch (DataAccessException e) {
            throw new InvalidInputException("name", "NAME_TAKEN");
        }
    }

    public UserDto getByNameOrEmail(String name) {
        Record record = ctx
            .select()
            .from(USER)
            .where(
                USER.EMAIL.isNotNull(),
                USER.NAME.equal(name).or(USER.EMAIL.equal(name)),
                USER.EMAIL_VERIFIED.isTrue()
            )
            .fetchOne();
        return UserDto.fromRecord(record);
    }

    public UserDto getByName(String name) {
        Record record = ctx
            .select()
            .from(USER)
            .where(USER.NAME.equal(name))
            .fetchOne();
        return UserDto.fromRecord(record);
    }

    public UserDto getById(long id) {
        Record record = ctx
            .select()
            .from(USER)
            .where(USER.ID.equal(id))
            .fetchOne();
        return UserDto.fromRecord(record);
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
        boolean success = ctx
            .update(USER)
            .set(changeMap)
            .where(USER.ID.equal(id))
            .execute() == 1;
        if (success) {
            Record record = ctx
                .selectFrom(USER)
                .where(USER.ID.equal(id))
                .fetchOne();
            userDto = UserDto.fromRecord(record);
        }
        return userDto;
    }

    public void setColor(long id, String color) {
        ctx
            .update(USER)
            .set(USER.COLOR, color)
            .where(USER.ID.equal(id))
            .execute();
    }

    public DataPage<UserData> getAllPaged(int page, int pageLength) {
        List<UserData> data = ctx
            .select(
                USER.ID, USER.NAME, USER.ROLE, USER.COLOR, USER.BANNED, USER.RENAME_AVAILABLE,
                USER.EMAIL, USER.EMAIL_VERIFIED, USER.CHECK_IP,
                DSL.groupConcat(USERAUTH.SERVICE).as("authServices"),
                DSL.groupConcat(DSL.coalesce(USERAUTH.AUTH_NAME, "")).as("authNames")
            )
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
        return new DataPage<>(data, page, Pages.pageCount(pageLength, ctx.fetchCount(USER)));
    }

    public DataPage<UserData> searchPaged(Integer page, int pageLength, String nameParam) {
        List<UserData> data = ctx
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
            Pages.pageCount(pageLength, ctx.fetchCount(USER, USER.NAME.like(nameParam, '!'))));
    }

    public List<UserDto> searchSimple(int pageLength, String nameParam) {
        return ctx
            .selectFrom(USER)
            .where(USER.NAME.like(nameParam, '!'))
            .groupBy(USER.ID)
            .orderBy(USER.ID)
            .limit(pageLength)
            .fetch()
            .stream()
            .map(UserDto::fromRecord)
            .collect(Collectors.toList());
    }

    public boolean delete(UserDto user) {
        return ctx
            .delete(USER)
            .where(USER.ID.equal(user.getId()))
            .execute() == 1;
    }

    public boolean checkName(String username) {
        return ctx
            .selectOne()
            .from(USER)
            .where(USER.NAME.equal(username))
            .fetchOne() == null;
    }

    public UserData fetchData(long id) {
        UserData result = null;
        Record record = ctx
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
        return result;
    }

    public List<UserDto> getAdmins() {
        return ctx
            .select()
            .from(USER)
            .where(USER.ROLE.in("ADMIN", "SUPERADMIN"))
            .fetch()
            .stream()
            .map(UserDto::fromRecord)
            .collect(Collectors.toList());
    }

    public void setCheckIp(UserDto user, boolean value) {
        int rows = ctx
            .update(USER)
            .set(USER.CHECK_IP, value)
            .where(USER.ID.equal(user.getId()))
            .execute();
        if (rows == 0) {
            throw new EntityNotFoundException("user");
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
