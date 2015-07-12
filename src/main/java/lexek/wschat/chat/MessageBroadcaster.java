package lexek.wschat.chat;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lexek.wschat.services.AbstractService;
import lexek.wschat.services.HistoryService;
import lexek.wschat.util.LoggingExceptionHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MessageBroadcaster extends AbstractService {
    private final Disruptor<MessageEvent> disruptor;
    private final RingBuffer<MessageEvent> ringBuffer;

    public MessageBroadcaster(HistoryService historyService, ConnectionManager connectionManager) {
        super("messageBroadcaster", ImmutableList.<String>of());
        EventFactory<MessageEvent> eventFactory = MessageEvent::new;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("MESSAGE_BROADCASTER_%d").build();
        this.disruptor = new Disruptor<>(
            eventFactory,
            64,
            Executors.newFixedThreadPool(2, threadFactory),
            ProducerType.MULTI,
            new BlockingWaitStrategy()
        );
        this.ringBuffer = this.disruptor.getRingBuffer();
        this.disruptor.handleExceptionsWith(new LoggingExceptionHandler());
        this.disruptor.handleEventsWith(historyService, connectionManager);
    }

    public void submitMessage(Message message, Connection connection, BroadcastFilter filter) {
        long sequence = ringBuffer.next();
        MessageEvent event = ringBuffer.get(sequence);
        event.setMessage(message);
        event.setConnection(connection);
        event.setBroadcastFilter(filter);
        ringBuffer.publish(sequence);
    }

    public void submitMessage(Message message, Connection connection) {
        submitMessage(message, connection, BroadcastFilter.NO_FILTER);
    }

    @Override
    protected void start0() {
        disruptor.start();
    }

    @Override
    public void performAction(String action) {

    }

    @Override
    public void stop() {
        disruptor.shutdown();
    }

    @Override
    public HealthCheck getHealthCheck() {
        return new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy();
            }
        };
    }

    @Override
    public void registerMetrics(MetricRegistry metricRegistry) {
        metricRegistry.register(this.getName() + ".queue.remainingCapacity", (Gauge<Long>) ringBuffer::remainingCapacity);
        metricRegistry.register(this.getName() + ".queue.bufferSize", (Gauge<Integer>) ringBuffer::getBufferSize);
    }
}
