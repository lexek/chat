package lexek.wschat.services;

import com.google.common.collect.ImmutableSet;
import com.lmax.disruptor.EventHandler;
import lexek.wschat.chat.*;
import lexek.wschat.db.dao.HistoryDao;
import lexek.wschat.db.jooq.tables.pojos.History;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.HistoryData;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static lexek.wschat.chat.Message.Keys;

public class HistoryService implements EventHandler<MessageEvent> {
    private static final Set<MessageType> STORE_TYPES = ImmutableSet.of(
            MessageType.MSG,
            MessageType.ME,
            MessageType.MSG_EXT,
            MessageType.BAN,
            MessageType.TIMEOUT,
            MessageType.CLEAR,
            MessageType.CLEAR_EXT,
            MessageType.LIKE
    );

    private final int maxHistory;
    private final HistoryDao historyDao;

    public HistoryService(int maxHistory, HistoryDao historyDao) {
        this.maxHistory = maxHistory;
        this.historyDao = historyDao;
    }

    public DataPage<HistoryData> getAllPagedAsJson(long roomId, int page, int pageLength) {
        return historyDao.getAll(roomId, page, pageLength);
    }

    public DataPage<HistoryData> getAllPagedAsJson(long roomId, int page, int pageLength, List<String> users) {
        return historyDao.getAllForUsers(roomId, page, pageLength, users);
    }

    public List<HistoryData> getLast20(long roomId) {
        return historyDao.getLast20(roomId);
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        final Message message = event.getMessage();
        if (event.getBroadcastFilter().getType() == BroadcastFilter.Type.ROOM) {
            final Room room = (Room) event.getBroadcastFilter().getData();
            if (STORE_TYPES.contains(message.getType())) {
                store(message, event.getConnection(), room);
            } else if (message.getType() == MessageType.CLEAR_ROOM) {
                room.getHistory().clear();
                historyDao.hideRoomMessages(room.getId(), System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));
            }
        }
    }

    private void store(Message message, Connection connection, Room room) {
        MessageType type = message.getType();
        User user = connection.getUser();
        if (type == MessageType.MSG || type == MessageType.ME) {
            historyDao.add(new History(null, room.getId(), user.getId(), message.get(Keys.TIME),
                    message.getType().toString(), message.get(Keys.TEXT), false));
        } else if (type == MessageType.CLEAR || type == MessageType.BAN || type == MessageType.TIMEOUT) {
            long t = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
            String userName = message.get(Keys.NAME);
            History msg = new History(null, room.getId(), user.getId(), t, message.getType().toString(), userName, false);
            historyDao.hideUserMessages(msg, userName, t);
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
