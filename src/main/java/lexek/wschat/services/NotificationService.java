package lexek.wschat.services;

import lexek.wschat.chat.*;
import lexek.wschat.db.dao.PendingNotificationDao;
import lexek.wschat.db.model.Email;
import lexek.wschat.db.model.UserDto;

import java.util.List;

public class NotificationService implements RoomJoinedEventListener {
    private final ConnectionManager connectionManager;
    private final UserService userService;
    private final MessageBroadcaster messageBroadcaster;
    private final EmailService emailService;
    private final PendingNotificationDao pendingNotificationDao;

    public NotificationService(ConnectionManager connectionManager,
                               UserService userService,
                               MessageBroadcaster messageBroadcaster,
                               EmailService emailService,
                               PendingNotificationDao pendingNotificationDao) {
        this.connectionManager = connectionManager;
        this.userService = userService;
        this.messageBroadcaster = messageBroadcaster;
        this.emailService = emailService;
        this.pendingNotificationDao = pendingNotificationDao;
    }

    public void notify(UserDto user, String summary, String description, boolean sendEmail) {
        if (connectionManager.anyConnection(connection -> user.getId().equals(connection.getUser().getId()))) {
            messageBroadcaster.submitMessage(
                Message.infoMessage(description),
                Connection.STUB_CONNECTION,
                new UserIdFilter(user.getId()));
        } else {
            pendingNotificationDao.add(user.getId(), description);
        }
        if (sendEmail && user.getEmail() != null && user.isEmailVerified()) {
            emailService.sendEmail(new Email(
                user.getEmail(),
                summary,
                description
            ));
        }
    }

    public void notifyAdmins(String summary, String description, boolean sendEmail) {
        userService.getAdmins().forEach(admin -> notify(admin, summary, description, sendEmail));
    }

    @Override
    public void joined(Connection connection, Chatter chatter, Room room) {
        if (chatter.hasRole(LocalRole.USER)) {
            List<String> notifications = pendingNotificationDao.getPendingNotifications(connection.getUser().getId());
            if (notifications != null) {
                for (String notification : notifications) {
                    connection.send(Message.infoMessage(notification));
                }
            }
        }
    }
}
