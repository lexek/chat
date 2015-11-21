package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Chatter;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.services.AnnouncementService;

public class SendAnnouncementsOnEventListener implements EventListener {
    private final AnnouncementService announcementService;

    public SendAnnouncementsOnEventListener(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        announcementService.sendAnnouncements(connection, room);
    }
}
