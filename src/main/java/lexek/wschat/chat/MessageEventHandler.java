package lexek.wschat.chat;

import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface MessageEventHandler {
    void onEvent(Message message, BroadcastFilter filter) throws Exception;
}
