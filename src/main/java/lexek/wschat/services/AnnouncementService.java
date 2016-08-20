package lexek.wschat.services;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.model.Message;
import lexek.wschat.db.dao.AnnouncementDao;
import lexek.wschat.db.jooq.tables.pojos.Announcement;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.tx.Transactional;
import lexek.wschat.services.managed.AbstractManagedService;
import lexek.wschat.services.managed.InitStage;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AnnouncementService extends AbstractManagedService {
    private final AnnouncementDao announcementDao;
    private final JournalService journalService;
    private final MessageBroadcaster messageBroadcaster;
    private final ScheduledExecutorService scheduledExecutor;
    private final Multimap<Room, Announcement> roomAnnouncements = HashMultimap.create();
    private final Runnable task;
    private long lastBroadcast;

    @Inject
    public AnnouncementService(AnnouncementDao announcementDao,
                               JournalService journalService, RoomManager roomManager,
                               MessageBroadcaster messageBroadcaster,
                               ScheduledExecutorService scheduledExecutor) {
        super("announcements", InitStage.SERVICES);
        this.journalService = journalService;
        this.messageBroadcaster = messageBroadcaster;
        this.scheduledExecutor = scheduledExecutor;
        this.announcementDao = announcementDao;
        this.lastBroadcast = System.currentTimeMillis();
        List<Announcement> announcements = announcementDao.getAll();
        for (Announcement announcement : announcements) {
            Room room = roomManager.getRoomInstance(announcement.getRoomId());
            if (room != null) {
                roomAnnouncements.put(room, announcement);
            }
        }
        task = () -> {
            lastBroadcast = System.currentTimeMillis();
            try {
                for (Map.Entry<Room, Announcement> entry : roomAnnouncements.entries()) {
                    messageBroadcaster.submitMessage(
                        Message.infoMessage(entry.getValue().getText()),
                        entry.getKey().FILTER
                    );
                }
            } catch (Exception e) {
                logger.warn("", e);
            }
        };
    }

    @Transactional
    public Announcement announce(String text, Room room, UserDto admin) {
        Announcement announcement = new Announcement(null, room.getId(), true, text);
        announcementDao.add(announcement);
        if (announcement.getId() != null) {
            roomAnnouncements.put(room, announcement);
            messageBroadcaster.submitMessage(Message.infoMessage(announcement.getText()), room.FILTER);
            journalService.newAnnouncement(admin, room, announcement);
        }
        return announcement;
    }

    public Announcement announceWithoutSaving(String text, Room room) {
        messageBroadcaster.submitMessage(Message.infoMessage(text), room.FILTER);
        return new Announcement(null, room.getId(), true, text);
    }

    public void sendAnnouncements(Connection connection, Room room) {
        for (Announcement announcement : roomAnnouncements.get(room)) {
            connection.send(Message.infoMessage(announcement.getText(), room.getName()));
        }
    }

    @Transactional
    public void setInactive(long id, UserDto admin) {
        Map.Entry<Room, Announcement> deleteEntry = null;
        for (Map.Entry<Room, Announcement> entry : roomAnnouncements.entries()) {
            if (entry.getValue().getId().equals(id)) {
                deleteEntry = entry;
            }
        }
        if (deleteEntry != null) {
            roomAnnouncements.remove(deleteEntry.getKey(), deleteEntry.getValue());
            journalService.inactiveAnnouncement(admin, deleteEntry.getKey(), deleteEntry.getValue());
        } else {
            throw new EntityNotFoundException("Announcement not found");
        }
        announcementDao.setInactive(id);
    }

    public Collection<Announcement> getAnnouncements(Room room) {
        return roomAnnouncements.get(room);
    }

    @Override
    public void start() {
        scheduledExecutor.scheduleWithFixedDelay(task, 0, 30, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        scheduledExecutor.shutdownNow();
    }

    @Override
    public HealthCheck getHealthCheck() {
        return new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return scheduledExecutor.isShutdown() || scheduledExecutor.isTerminated() ?
                    Result.unhealthy("executor is terminated") :
                    Result.healthy();
            }
        };
    }

    @Override
    public void registerMetrics(MetricRegistry metricRegistry) {
        metricRegistry.register(this.getName() + ".lastBroadcast", (Gauge<Long>) () -> lastBroadcast);
    }

}
