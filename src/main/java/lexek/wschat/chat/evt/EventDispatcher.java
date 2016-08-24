package lexek.wschat.chat.evt;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.Chatter;
import lexek.wschat.services.managed.AbstractManagedService;
import lexek.wschat.services.managed.InitStage;
import lexek.wschat.util.LoggingExceptionHandler;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.StreamSupport;

@Service
public class EventDispatcher extends AbstractManagedService {
    private final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);
    private final Disruptor<Event> disruptor;
    private final RingBuffer<Event> ringBuffer;
    private final Multimap<ChatEventType, EventListener> listeners = LinkedHashMultimap.create();

    public EventDispatcher() {
        super("eventDispatcher", InitStage.CORE);
        EventFactory<Event> eventFactory = Event::new;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("NOTIFICATIONS_%d").build();
        this.disruptor = new Disruptor<>(
            eventFactory,
            32,
            Executors.newSingleThreadExecutor(threadFactory),
            ProducerType.MULTI,
            new BlockingWaitStrategy()
        );
        this.ringBuffer = this.disruptor.getRingBuffer();
        this.disruptor.handleExceptionsWith(new LoggingExceptionHandler());
        this.disruptor.handleEventsWith(new NotificationServiceWorker());
    }

    @Inject
    public void init(Iterable<EventListener> eventListeners) {
        StreamSupport.stream(eventListeners.spliterator(), false)
            .sorted((o1, o2) -> o1.getOrder() - o2.getOrder())
            .forEach(e -> registerListener(e.getEventType(), e));
    }

    @Override
    public void start() {
        this.disruptor.start();
    }

    @Override
    public void stop() {
        this.disruptor.shutdown();
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

    public void registerListener(ChatEventType eventType, EventListener listener) {
        this.listeners.put(eventType, listener);
    }

    public void joinedRoom(Connection connection, Chatter chatter, Room room) {
        long sequence = ringBuffer.next();
        Event event = ringBuffer.get(sequence);
        event.setEventType(ChatEventType.JOIN);
        event.setConnection(connection);
        event.setChatter(chatter);
        event.setRoom(room);
        ringBuffer.publish(sequence);
    }

    public void connected(Connection connection) {
        long sequence = ringBuffer.next();
        Event event = ringBuffer.get(sequence);
        event.setEventType(ChatEventType.CONNECT);
        event.setConnection(connection);
        event.setChatter(null);
        event.setRoom(null);
        ringBuffer.publish(sequence);
    }

    private static class Event {
        private ChatEventType eventType;
        private Connection connection;
        private Chatter chatter;
        private Room room;

        public ChatEventType getEventType() {
            return eventType;
        }

        public void setEventType(ChatEventType eventType) {
            this.eventType = eventType;
        }

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

        public Chatter getChatter() {
            return chatter;
        }

        public void setChatter(Chatter chatter) {
            this.chatter = chatter;
        }

        public Room getRoom() {
            return room;
        }

        public void setRoom(Room room) {
            this.room = room;
        }
    }

    private class NotificationServiceWorker implements EventHandler<Event> {
        @Override
        public void onEvent(final Event event, long l, boolean b) throws Exception {
            listeners
                .get(event.getEventType())
                .forEach(listener -> {
                    try {
                        listener.onEvent(event.getConnection(), event.getChatter(), event.getRoom());
                    } catch (Exception e) {
                        logger.warn("There was exception while processing event {}", event, e);
                    }
                });
        }
    }
}
