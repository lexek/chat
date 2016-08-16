package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.ChatEventType;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.services.AnnouncementService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class SendAnnouncementsOnEventListener implements EventListener {
    private final AnnouncementService announcementService;

    @Inject
    public SendAnnouncementsOnEventListener(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        announcementService.sendAnnouncements(connection, room);
    }

    @Override
    public int getOrder() {
        return 4;
    }

    @Override
    public ChatEventType getEventType() {
        return ChatEventType.JOIN;
    }
}
