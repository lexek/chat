package lexek.wschat.db.dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.jooq.tables.records.PollOptionRecord;
import lexek.wschat.db.jooq.tables.records.PollRecord;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.services.poll.Poll;
import lexek.wschat.services.poll.PollOption;
import lexek.wschat.services.poll.PollState;
import lexek.wschat.util.Pages;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.Poll.POLL;
import static lexek.wschat.db.jooq.tables.PollAnswer.POLL_ANSWER;
import static lexek.wschat.db.jooq.tables.PollOption.POLL_OPTION;

public class PollDao {
    private final DataSource dataSource;

    public PollDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Poll add(final String question, final ImmutableList<PollOption> options, final long roomId) {
        Poll result = null;
        try (Connection connection = dataSource.getConnection()) {
            Long id = DSL.using(connection).transactionResult(txConf -> {
                Long id1 = DSL.using(txConf)
                    .insertInto(POLL, POLL.QUESTION, POLL.ROOM_ID, POLL.OPEN)
                    .values(question, roomId, true)
                    .returning(POLL.ID)
                    .fetchOne().getId();
                if (id1 != null) {
                    DSL.using(txConf)
                        .batchInsert(options
                            .stream()
                            .map(option -> new PollOptionRecord(id1, option.getOptionId(), option.getText()))
                            .collect(Collectors.toList()))
                        .execute();
                }
                return id1;
            });
            if (id != null) {
                result = new Poll(id, question, options);
            }
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
    }

    public void vote(long pollId, long userId, int optionId) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .insertInto(POLL_ANSWER, POLL_ANSWER.POLL_ID, POLL_ANSWER.USER_ID, POLL_ANSWER.SELECTED_OPTION)
                .values(pollId, userId, optionId)
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public void closePoll(long pollId) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection)
                .update(POLL)
                .set(POLL.OPEN, false)
                .where(POLL.ID.equal(pollId))
                .execute();
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public Map<Long, PollState> getAllPolls() {
        Map<Long, PollState> polls = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            Result<PollRecord> r = DSL.using(connection)
                .select(POLL.ID, POLL.OPEN, POLL.QUESTION, POLL.ROOM_ID)
                .from(POLL)
                .where(POLL.OPEN.isTrue())
                .fetchInto(POLL);
            if (r != null) {
                for (PollRecord record : r) {
                    Result<? extends Record> options = DSL.using(connection)
                        .select(POLL_OPTION.OPTION, POLL_OPTION.TEXT, DSL.count(POLL_ANSWER.SELECTED_OPTION).as("votes"))
                        .from(POLL_OPTION.leftOuterJoin(POLL_ANSWER).on(POLL_OPTION.POLL_ID.equal(POLL_ANSWER.POLL_ID)).and(POLL_OPTION.OPTION.equal(POLL_ANSWER.SELECTED_OPTION)))
                        .where(POLL_OPTION.POLL_ID.equal(record.getId()))
                        .groupBy(POLL_OPTION.OPTION)
                        .orderBy(POLL_OPTION.OPTION.asc())
                        .fetch();
                    List<Long> voted = DSL.using(connection)
                        .selectDistinct(POLL_ANSWER.USER_ID)
                        .from(POLL_ANSWER)
                        .where(POLL_ANSWER.POLL_ID.equal(record.getId()))
                        .fetch(POLL_ANSWER.USER_ID, Long.class);
                    if (options != null) {
                        ImmutableList.Builder<PollOption> pollOptions = ImmutableList.builder();
                        long[] votes = new long[options.size()];
                        for (Record optionRecord : options) {
                            int i = optionRecord.getValue(POLL_OPTION.OPTION);
                            pollOptions.add(new PollOption(i, optionRecord.getValue(POLL_OPTION.TEXT)));
                            votes[i] = (int) optionRecord.getValue("votes");
                        }
                        Poll poll = new Poll(record.getId(), record.getQuestion(), pollOptions.build());
                        PollState pollState = new PollState(poll, votes, Sets.newHashSet(voted));
                        polls.put(record.getRoomId(), pollState);
                    }
                }
            }
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return polls;
    }

    public DataPage<PollState> getOldPolls(long roomId, int page) {
        try (Connection connection = dataSource.getConnection()) {
            List<PollState> polls = new ArrayList<>();
            int count = DSL.using(connection).fetchCount(POLL, POLL.OPEN.isFalse().and(POLL.ROOM_ID.equal(roomId)));
            Result<PollRecord> r = DSL.using(connection)
                .select(POLL.ID, POLL.OPEN, POLL.QUESTION)
                .from(POLL)
                .where(POLL.OPEN.isFalse().and(POLL.ROOM_ID.equal(roomId)))
                .orderBy(POLL.ID.desc())
                .limit(page * 5, 5)
                .fetchInto(POLL);
            if (r != null) {
                for (PollRecord record : r) {
                    Result<? extends Record> options = DSL.using(connection)
                        .select(POLL_OPTION.OPTION, POLL_OPTION.TEXT, DSL.count(POLL_ANSWER.SELECTED_OPTION).as("votes"))
                        .from(POLL_OPTION.leftOuterJoin(POLL_ANSWER).on(POLL_OPTION.POLL_ID.equal(POLL_ANSWER.POLL_ID)).and(POLL_OPTION.OPTION.equal(POLL_ANSWER.SELECTED_OPTION)))
                        .where(POLL_OPTION.POLL_ID.equal(record.getId()))
                        .groupBy(POLL_OPTION.OPTION)
                        .orderBy(POLL_OPTION.OPTION.asc())
                        .fetch();
                    if (options != null) {
                        ImmutableList.Builder<PollOption> pollOptions = ImmutableList.builder();
                        long[] votes = new long[options.size()];
                        for (Record optionRecord : options) {
                            int i = optionRecord.getValue(POLL_OPTION.OPTION);
                            pollOptions.add(new PollOption(i, optionRecord.getValue(POLL_OPTION.TEXT)));
                            votes[i] = (int) optionRecord.getValue("votes");
                        }
                        Poll poll = new Poll(record.getId(), record.getQuestion(), pollOptions.build());
                        PollState pollState = new PollState(poll, votes, null);
                        polls.add(pollState);
                    }
                }
            }
            return new DataPage<>(polls, page, Pages.pageCount(5, count));
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }
}
