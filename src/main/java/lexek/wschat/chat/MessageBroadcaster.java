package lexek.wschat.chat;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.services.managed.AbstractManagedService;
import lexek.wschat.services.managed.InitStage;
import lexek.wschat.util.LoggingExceptionHandler;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Service
public class MessageBroadcaster extends AbstractManagedService {
    private final Disruptor<MessageEvent> disruptor;
    private final RingBuffer<MessageEvent> ringBuffer;

    @Inject
    public MessageBroadcaster() {
        super("messageBroadcaster", InitStage.SERVICES);
        EventFactory<MessageEvent> eventFactory = MessageEvent::new;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("MESSAGE_BROADCASTER_%d").build();
        this.disruptor = new Disruptor<>(
            eventFactory,
            64,
            Executors.newCachedThreadPool(threadFactory),
            ProducerType.MULTI,
            new BlockingWaitStrategy()
        );
        this.ringBuffer = this.disruptor.getRingBuffer();
        this.disruptor.handleExceptionsWith(new LoggingExceptionHandler());
    }

    @Inject
    public void init(IterableProvider<MessageEventHandler> handlers) {
        handlers.forEach(this::registerConsumer);
    }

    /**
     * Submits message to broadcast
     *
     * @param message message to send
     * @param filter  how to filter connections
     */
    public void submitMessage(Message message, BroadcastFilter filter) {
        if (filter == null) {
            throw new NullPointerException("filter");
        }
        if (message == null) {
            throw new NullPointerException("message");
        }
        long sequence = ringBuffer.next();
        MessageEvent event = ringBuffer.get(sequence);
        event.setMessage(message);
        event.setBroadcastFilter(filter);
        ringBuffer.publish(sequence);
    }

    /**
     * Will submit message with {@link BroadcastFilter#NO_FILTER} as filter
     *
     * @param message message to send
     */
    public void submitMessage(Message message) {
        submitMessage(message, BroadcastFilter.NO_FILTER);
    }

    public void registerConsumer(MessageEventHandler consumer) {
        this.disruptor.handleEventsWith((EventHandler) consumer);
    }

    @Override
    public void start() {
        disruptor.start();
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
