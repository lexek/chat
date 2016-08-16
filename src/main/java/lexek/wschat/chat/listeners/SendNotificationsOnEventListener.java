package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.ChatEventType;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.services.NotificationService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

@Service
public class SendNotificationsOnEventListener implements EventListener {
    private final NotificationService notificationService;

    @Inject
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

    @Override
    public int getOrder() {
        return 6;
    }

    @Override
    public ChatEventType getEventType() {
        return ChatEventType.JOIN;
    }
}
