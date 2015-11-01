package lexek.wschat.services;

import lexek.wschat.chat.*;

public class IgnoreJoinListener implements RoomJoinedEventListener {
    private final IgnoreService ignoreService;

    public IgnoreJoinListener(IgnoreService ignoreService) {
        this.ignoreService = ignoreService;
    }

    @Override
    public void joined(Connection connection, Chatter chatter, Room room) {
        User user = connection.getUser();
        if (user.hasRole(GlobalRole.USER)) {
            connection.send(Message.ignoredMessage(ignoreService.getIgnoredNames(connection.getUser())));
        }
    }
}
