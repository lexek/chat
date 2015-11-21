package lexek.wschat.services;

import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Message;
import lexek.wschat.chat.filters.BroadcastFilter;

public interface MessageConsumerService {
    void consume(Connection connection, Message message, BroadcastFilter filter);
}
