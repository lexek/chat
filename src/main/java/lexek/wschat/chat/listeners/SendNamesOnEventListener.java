package lexek.wschat.chat.listeners;

import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.evt.ChatEventType;
import lexek.wschat.chat.evt.EventListener;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import org.jvnet.hk2.annotations.Service;

@Service
public class SendNamesOnEventListener implements EventListener {
    @Override
    public void onEvent(Connection connection, Chatter chatter, Room room) {
        if (connection.isNeedNames()) {
            ImmutableList.Builder<Chatter> users = ImmutableList.builder();
            room.getOnlineChatters().stream().filter(c -> c.hasRole(LocalRole.USER)).forEach(users::add);
            connection.send(Message.namesMessage(room.getName(), users.build(), chatter.getName()));
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public ChatEventType getEventType() {
        return ChatEventType.JOIN;
    }
}
