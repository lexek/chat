package lexek.wschat.services;

import com.google.common.collect.ImmutableSet;
import com.lmax.disruptor.EventHandler;
import lexek.wschat.chat.MessageEvent;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.db.dao.HistoryDao;
import lexek.wschat.db.jooq.tables.pojos.History;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.HistoryData;
import lexek.wschat.db.model.UserDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HistoryService implements EventHandler<MessageEvent> {
    private static final Set<MessageType> STORE_TYPES = ImmutableSet.of(
        MessageType.MSG,
        MessageType.ME,
        MessageType.MSG_EXT,
        MessageType.BAN,
        MessageType.TIMEOUT,
        MessageType.CLEAR,
        MessageType.PROXY_CLEAR,
        MessageType.LIKE,
        MessageType.TWEET
    );

    private final int maxHistory;
    private final HistoryDao historyDao;
    private final UserService userService;

    public HistoryService(int maxHistory, HistoryDao historyDao, UserService userService) {
        this.maxHistory = maxHistory;
        this.historyDao = historyDao;
        this.userService = userService;
    }

    public DataPage<HistoryData> getAllPagedAsJson(long roomId, int page, int pageLength, Optional<List<String>> users,
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

    private void store(Message message, Room room) {
        MessageType type = message.getType();
        if (type == MessageType.MSG || type == MessageType.ME) {
            long userId = message.get(MessageProperty.USER_ID);
            historyDao.add(new History(null, room.getId(), userId, message.get(MessageProperty.TIME),
                message.getType().toString(), message.get(MessageProperty.TEXT), false));
        } else if (type == MessageType.CLEAR || type == MessageType.BAN || type == MessageType.TIMEOUT) {
            //todo: find better solution, but this works for now since it's not really frequent event type
            UserDto mod = userService.fetchByName(message.get(MessageProperty.MOD));
            long t = System.currentTimeMillis();
            String userName = message.get(MessageProperty.NAME);
            historyDao.hideUserMessages(
                new History(null, room.getId(), mod.getId(), t, message.getType().toString(), userName, false),
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
}
