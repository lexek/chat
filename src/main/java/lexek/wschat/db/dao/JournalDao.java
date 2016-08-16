package lexek.wschat.db.dao;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.JournalEntry;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.util.Pages;
import org.jooq.Condition;
import org.jooq.Operator;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.Tables.USER;
import static lexek.wschat.db.jooq.tables.Journal.JOURNAL;

@Service
public class JournalDao {
    private static final Set<String> GLOBAL_ACTIONS = ImmutableSet.of(
        "USER_UPDATE",
        "NAME_CHANGE",
        "NEW_EMOTICON",
        "IMAGE_EMOTICON",
        "DELETED_EMOTICON",
        "NEW_ROOM",
        "DELETED_ROOM",
        "PASSWORD"
    );
    private final DataSource dataSource;

    @Inject
    public JournalDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void add(JournalEntry journal) {
        UserDto user = journal.getUser();
        UserDto admin = journal.getAdmin();
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .insertInto(JOURNAL)
                .columns(JOURNAL.USER_ID, JOURNAL.ADMIN_ID, JOURNAL.ACTION, JOURNAL.ACTION_DESCRIPTION,
                    JOURNAL.TIME, JOURNAL.ROOM_ID)
                .values(user != null ? user.getId() : null,
                    admin != null ? admin.getId() : null,
                    journal.getAction(),
                    journal.getActionDescription(),
                    new Timestamp(journal.getTime()),
                    journal.getRoomId())
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public DataPage<JournalEntry> fetchAllGlobal(
        int page,
        int pageSize,
        Optional<Set<String>> types,
        Optional<Long> userId,
        Optional<Long> adminId
    ) {
        try (Connection connection = dataSource.getConnection()) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(JOURNAL.ACTION.in(types.isPresent() ? types.get() : GLOBAL_ACTIONS));
            userId.ifPresent(value -> conditions.add(JOURNAL.USER_ID.equal(value)));
            adminId.ifPresent(value -> conditions.add(JOURNAL.ADMIN_ID.equal(value)));

            List<JournalEntry> data = DSL.using(connection)
                .selectFrom(JOURNAL
                    .leftOuterJoin(USER).on(JOURNAL.USER_ID.equal(USER.ID))
                    .leftOuterJoin(USER.as("admin")).on(JOURNAL.ADMIN_ID.equal(USER.as("admin").ID)))
                .where(conditions)
                .orderBy(JOURNAL.ID.desc())
                .limit(page * pageSize, pageSize)
                .fetch()
                .stream()
                .map(record -> new JournalEntry(
                    UserDto.fromRecord(record.into(USER)),
                    UserDto.fromRecord(record.into(USER.as("admin"))),
                    record.getValue(JOURNAL.ACTION),
                    record.getValue(JOURNAL.ACTION_DESCRIPTION),
                    record.getValue(JOURNAL.TIME).getTime(),
                    record.getValue(JOURNAL.ROOM_ID)))
                .collect(Collectors.toList());
            int count = DSL.using(connection).fetchCount(
                JOURNAL,
                DSL.condition(Operator.AND, conditions)
            );
            return new DataPage<>(data, page, Pages.pageCount(pageSize, count));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public DataPage<JournalEntry> fetchAllForRoom(
        int page,
        int pageSize,
        long roomId,
        Optional<Set<String>> types,
        Optional<Long> userId,
        Optional<Long> adminId
    ) {
        try (Connection connection = dataSource.getConnection()) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(JOURNAL.ROOM_ID.equal(roomId));
            types.ifPresent(value -> conditions.add(JOURNAL.ACTION.in(value)));
            userId.ifPresent(value -> conditions.add(JOURNAL.USER_ID.equal(value)));
            adminId.ifPresent(value -> conditions.add(JOURNAL.ADMIN_ID.equal(value)));

            List<JournalEntry> data = DSL.using(connection)
                .selectFrom(JOURNAL
                    .leftOuterJoin(USER).on(JOURNAL.USER_ID.equal(USER.ID))
                    .leftOuterJoin(USER.as("admin")).on(JOURNAL.ADMIN_ID.equal(USER.as("admin").ID)))
                .where(conditions)
                .orderBy(JOURNAL.ID.desc())
                .limit(page * pageSize, pageSize)
                .fetch()
                .stream()
                .map(record -> new JournalEntry(
                    UserDto.fromRecord(record.into(USER)),
                    UserDto.fromRecord(record.into(USER.as("admin"))),
                    record.getValue(JOURNAL.ACTION),
                    record.getValue(JOURNAL.ACTION_DESCRIPTION),
                    record.getValue(JOURNAL.TIME).getTime(),
                    record.getValue(JOURNAL.ROOM_ID)))
                .collect(Collectors.toList());
            int count = DSL.using(connection).fetchCount(
                JOURNAL,
                DSL.condition(Operator.AND, conditions)
            );
            return new DataPage<>(data, page, Pages.pageCount(pageSize, count));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }
}
