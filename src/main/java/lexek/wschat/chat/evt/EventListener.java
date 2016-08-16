package lexek.wschat.chat.evt;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.Chatter;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface EventListener {
    void onEvent(Connection connection, Chatter chatter, Room room);

    int getOrder();

    ChatEventType getEventType();
}
