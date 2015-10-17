package lexek.wschat.services;

import com.lmax.disruptor.EventHandler;
import lexek.wschat.chat.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageConsumerServiceHandler implements EventHandler<MessageEvent> {
    private final Logger logger = LoggerFactory.getLogger(MessageConsumerServiceHandler.class);
    private final List<MessageConsumerService> consumers = new CopyOnWriteArrayList<>();

    public void register(MessageConsumerService consumer) {
        consumers.add(consumer);
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        for (MessageConsumerService consumer : consumers) {
            try {
                consumer.consume(event.getConnection(), event.getMessage(), event.getBroadcastFilter());
            } catch (Exception e) {
                logger.warn("exception while handling message for message consumer", e);
            }
        }
    }
}
