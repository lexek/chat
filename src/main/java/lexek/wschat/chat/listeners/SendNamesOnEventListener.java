package lexek.wschat.chat.listeners;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.*;
import lexek.wschat.chat.evt.EventListener;

public class SendNamesOnEventListener implements EventListener {
    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        if (connection.isNeedNames()) {
            ImmutableList.Builder<Chatter> users = ImmutableList.builder();
            room.getOnlineChatters().stream().filter(c -> c.hasRole(LocalRole.USER)).forEach(users::add);
            connection.send(Message.namesMessage(room.getName(), users.build()));
        }
    }
}
