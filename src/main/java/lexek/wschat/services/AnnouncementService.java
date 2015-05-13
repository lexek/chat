package lexek.wschat.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import lexek.wschat.chat.*;
import lexek.wschat.db.dao.AnnouncementDao;
import lexek.wschat.db.jooq.tables.pojos.Announcement;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AnnouncementService extends AbstractService {
    private final MessageBroadcaster messageBroadcaster;
    private final AnnouncementDao announcementDao;
    private final ScheduledExecutorService scheduledExecutor;
    private final Multimap<Room, Announcement> roomAnnouncements = HashMultimap.create();
    private final RoomManager roomManager;
    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            stateData = System.currentTimeMillis();
            try {
                for (Map.Entry<Room, Announcement> entry : roomAnnouncements.entries()) {
                    messageBroadcaster.submitMessage(
                            Message.infoMessage(entry.getValue().getText()),
                            Connection.STUB_CONNECTION,
                            entry.getKey().FILTER);
                }
            } catch (Exception e) {
                logger.warn("", e);
            }
        }
    };

    public AnnouncementService(AnnouncementDao announcementDao,
                               RoomManager roomManager,
                               MessageBroadcaster messageBroadcaster,
                               ScheduledExecutorService scheduledExecutor) {
        super("announcements", ImmutableList.of("force_announce"));
        this.messageBroadcaster = messageBroadcaster;
        this.scheduledExecutor = scheduledExecutor;
        this.roomManager = roomManager;
        this.announcementDao = announcementDao;
        stateData = System.currentTimeMillis();
        List<Announcement> announcements = announcementDao.getAll();
        for (Announcement announcement : announcements) {
            Room room = roomManager.getRoomInstance(announcement.getRoomId());
            if (room != null) {
                roomAnnouncements.put(room, announcement);
            }
        }
    }

    public void announce(Announcement announcement) {
        Room room = roomManager.getRoomInstance(announcement.getRoomId());
        if (room != null) {
            announcementDao.add(announcement);
            if (announcement.getId() != null) {
                roomAnnouncements.put(room, announcement);
                messageBroadcaster.submitMessage(Message.infoMessage(announcement.getText()), Connection.STUB_CONNECTION);
            }
        }
    }

    public void sendAnnouncements(Connection connection, Room room) {
        for (Announcement announcement : roomAnnouncements.get(room)) {
            connection.send(Message.infoMessage(announcement.getText(), room.getName()));
        }
    }

    public void setInactive(long id) {
        Map.Entry<Room, Announcement> deleteEntry = null;
        for (Map.Entry<Room, Announcement> entry : roomAnnouncements.entries()) {
            if (entry.getValue().getId().equals(id)) {
                deleteEntry = entry;
            }
        }
        if (deleteEntry != null) {
            roomAnnouncements.remove(deleteEntry.getKey(), deleteEntry.getValue());
        }
        announcementDao.setInactive(id);
    }

    public Collection<Announcement> getAnnouncements(Room room) {
        return roomAnnouncements.get(room);
    }

    @Override
    public void performAction(String action) {
        if (action.equals("force_announce")) {
            scheduledExecutor.submit(task);
        }
    }

    @Override
    protected void start0() {
        scheduledExecutor.scheduleWithFixedDelay(task, 0, 30, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        scheduledExecutor.shutdownNow();
    }
}
