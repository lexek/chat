package lexek.wschat.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import lexek.wschat.chat.MessageEvent;
import lexek.wschat.chat.MessageEventHandler;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.db.dao.HistoryDao;
import lexek.wschat.db.jooq.tables.pojos.History;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.HistoryData;
import lexek.wschat.db.model.UserDto;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class HistoryService implements MessageEventHandler {
    private static final Set<MessageType> STORE_TYPES = ImmutableSet.of(
        MessageType.MSG,
        MessageType.ME,
        MessageType.MSG_EXT,
        MessageType.BAN,
        MessageType.TIMEOUT,
        MessageType.CLEAR,
        MessageType.PROXY_CLEAR,
        MessageType.LIKE,
        MessageType.TWEET,
        MessageType.DONATION
    );

    private final int maxHistory;
    private final HistoryDao historyDao;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public HistoryService(
        @Named("history.pageLength") int maxHistory,
        HistoryDao historyDao,
        UserService userService
    ) {
        this.maxHistory = maxHistory;
        this.historyDao = historyDao;
        this.userService = userService;
    }

    public DataPage<HistoryData> getAllPagedAsJson(long roomId, int page, int pageLength, Optional<List<Long>> users,
                                                   Optional<Long> since, Optional<Long> until) {
        return historyDao.getAllForUsers(roomId, page, pageLength, users, since, until);
    }

    public List<HistoryData> getLastN(long roomId, int count) {
        return historyDao.getLastN(roomId, count);
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        final Message message = event.getMessage();
        if (event.getBroadcastFilter().getType() == BroadcastFilter.Type.ROOM) {
            final Room room = (Room) event.getBroadcastFilter().getData();
            if (STORE_TYPES.contains(message.getType())) {
                store(message, room);
            } else if (message.getType() == MessageType.CLEAR_ROOM) {
                room.getHistory().clear();
                historyDao.hideRoomMessages(room.getId(), System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));
            }
        }
    }

    private void store(Message message, Room room) throws JsonProcessingException {
        MessageType type = message.getType();
        if (type == MessageType.MSG || type == MessageType.ME) {
            long userId = message.get(MessageProperty.USER_ID);
            List<MessageNode> messageBody = message.get(MessageProperty.MESSAGE_NODES);
            Multiset<Long> stats = HashMultiset.create();
            collectEmoticons(messageBody, stats);
            historyDao.addWithStats(
                new History(
                    null,
                    room.getId(),
                    userId,
                    message.get(MessageProperty.TIME),
                    message.getType().toString(),
                    objectMapper.writeValueAsString(messageBody),
                    false,
                    false
                ),
                stats
            );

        } else if (type == MessageType.CLEAR || type == MessageType.BAN || type == MessageType.TIMEOUT) {
            //todo: find better solution, but this works for now since it's not really frequent event type
            UserDto mod = userService.fetchByName(message.get(MessageProperty.MOD));
            long t = System.currentTimeMillis();
            String userName = message.get(MessageProperty.NAME);
            historyDao.hideUserMessages(
                new History(null, room.getId(), mod.getId(), t, message.getType().toString(), userName, false, false),
                userName, t - TimeUnit.MINUTES.toMillis(10));
        }
        if (room != null) {
            List<Message> messages = room.getHistory();
            messages.add(message);
            if (messages.size() == maxHistory) {
                messages.remove(0);
            }
        }
    }

    private void collectEmoticons(List<MessageNode> nodes, Multiset<Long> result) {
        for (MessageNode node : nodes) {
            if (node.getType() == MessageNode.Type.EMOTICON) {
                result.add(node.getEmoticonId());
            }
            if (node.getChildren() != null) {
                collectEmoticons(node.getChildren(), result);
            }
        }
    }
}
