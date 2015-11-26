package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.services.NotificationService;

import java.util.List;

public class SendNotificationsOnEventListener implements EventListener {
    private final NotificationService notificationService;

    public SendNotificationsOnEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        if (connection.getUser().hasRole(GlobalRole.USER)) {
            List<String> notifications = notificationService.getPendingNotifications(connection.getUser());
            if (notifications != null) {
                for (String notification : notifications) {
                    connection.send(Message.infoMessage(notification));
                }
            }
        }
    }
}
