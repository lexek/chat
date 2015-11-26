package lexek.wschat.services;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;

public interface MessageConsumerService {
    void consume(Connection connection, Message message, BroadcastFilter filter);
}
