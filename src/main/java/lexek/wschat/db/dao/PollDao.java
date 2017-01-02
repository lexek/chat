package lexek.wschat.db.dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import lexek.wschat.db.jooq.tables.records.PollOptionRecord;
import lexek.wschat.db.jooq.tables.records.PollRecord;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.tx.Transactional;
import lexek.wschat.services.poll.Poll;
import lexek.wschat.services.poll.PollOption;
import lexek.wschat.services.poll.PollState;
import lexek.wschat.util.Pages;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.Poll.POLL;
import static lexek.wschat.db.jooq.tables.PollAnswer.POLL_ANSWER;
import static lexek.wschat.db.jooq.tables.PollOption.POLL_OPTION;

@Service
public class PollDao {
    private final DSLContext ctx;

    @Inject
    public PollDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Transactional
    public Poll add(final String question, final ImmutableList<PollOption> options, final long roomId) {
        Poll result = null;
        Long id = ctx
            .insertInto(POLL, POLL.QUESTION, POLL.ROOM_ID, POLL.OPEN)
            .values(question, roomId, true)
            .returning(POLL.ID)
            .fetchOne().getId();
        if (id != null) {
            ctx
                .batchInsert(options
                    .stream()
                    .map(option -> new PollOptionRecord(id, option.getOptionId(), option.getText()))
                    .collect(Collectors.toList()))
                .execute();
        }
        if (id != null) {
            result = new Poll(id, question, options);
        }
        return result;
    }

    public void vote(long pollId, long userId, int optionId) {
        ctx
            .insertInto(POLL_ANSWER, POLL_ANSWER.POLL_ID, POLL_ANSWER.USER_ID, POLL_ANSWER.SELECTED_OPTION)
            .values(pollId, userId, optionId)
            .execute();
    }

    public void closePoll(long pollId) {
        ctx
            .update(POLL)
            .set(POLL.OPEN, false)
            .where(POLL.ID.equal(pollId))
            .execute();
    }

    public Map<Long, PollState> getAllPolls() {
        Map<Long, PollState> polls = new HashMap<>();
        Result<PollRecord> r = ctx
            .select(POLL.ID, POLL.OPEN, POLL.QUESTION, POLL.ROOM_ID)
            .from(POLL)
            .where(POLL.OPEN.isTrue())
            .fetchInto(POLL);
        if (r != null) {
            for (PollRecord record : r) {
                Result<? extends Record> options = ctx
                    .select(POLL_OPTION.OPTION, POLL_OPTION.TEXT, DSL.count(POLL_ANSWER.SELECTED_OPTION).as("votes"))
                    .from(
                        POLL_OPTION
                            .leftOuterJoin(POLL_ANSWER)
                            .on(POLL_OPTION.POLL_ID.equal(POLL_ANSWER.POLL_ID))
                            .and(POLL_OPTION.OPTION.equal(POLL_ANSWER.SELECTED_OPTION))
                    )
                    .where(POLL_OPTION.POLL_ID.equal(record.getId()))
                    .groupBy(POLL_OPTION.OPTION)
                    .orderBy(POLL_OPTION.OPTION.asc())
                    .fetch();
                List<Long> voted = ctx
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
        return polls;
    }

    public DataPage<PollState> getOldPolls(long roomId, int page) {
        List<PollState> polls = new ArrayList<>();
        int count = ctx.fetchCount(POLL, POLL.OPEN.isFalse().and(POLL.ROOM_ID.equal(roomId)));
        Result<PollRecord> r = ctx
            .select(POLL.ID, POLL.OPEN, POLL.QUESTION)
            .from(POLL)
            .where(POLL.OPEN.isFalse().and(POLL.ROOM_ID.equal(roomId)))
            .orderBy(POLL.ID.desc())
            .limit(page * 5, 5)
            .fetchInto(POLL);
        if (r != null) {
            for (PollRecord record : r) {
                Result<? extends Record> options = ctx
                    .select(POLL_OPTION.OPTION, POLL_OPTION.TEXT, DSL.count(POLL_ANSWER.SELECTED_OPTION).as("votes"))
                    .from(
                        POLL_OPTION
                            .leftOuterJoin(POLL_ANSWER)
                            .on(POLL_OPTION.POLL_ID.equal(POLL_ANSWER.POLL_ID))
                            .and(POLL_OPTION.OPTION.equal(POLL_ANSWER.SELECTED_OPTION))
                    )
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
    }
}
