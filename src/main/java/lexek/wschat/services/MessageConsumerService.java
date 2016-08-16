package lexek.wschat.services;

import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface MessageConsumerService {
    void consume(Message message, BroadcastFilter filter);
}
