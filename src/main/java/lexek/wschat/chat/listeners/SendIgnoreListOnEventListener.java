package lexek.wschat.chat.listeners;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.ChatEventType;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.User;
import lexek.wschat.services.IgnoreService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class SendIgnoreListOnEventListener implements EventListener {
    private final IgnoreService ignoreService;

    @Inject
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

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatEventType getEventType() {
        return ChatEventType.CONNECT;
    }
}
