package lexek.wschat.chat.listeners;

import lexek.wschat.chat.*;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.services.IgnoreService;

public class SendIgnoreListOnEventListener implements EventListener {
    private final IgnoreService ignoreService;

    public SendIgnoreListOnEventListener(IgnoreService ignoreService) {
        this.ignoreService = ignoreService;
    }

    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        User user = connection.getUser();
        if (user.hasRole(GlobalRole.USER)) {
            connection.send(Message.ignoredMessage(ignoreService.getIgnoredNames(connection.getUser())));
        }
    }
}
