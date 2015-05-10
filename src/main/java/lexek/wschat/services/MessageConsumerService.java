package lexek.wschat.services;

import lexek.wschat.chat.BroadcastFilter;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Message;

public interface MessageConsumerService {
    void consume(Connection connection, Message message, BroadcastFilter filter);
}
