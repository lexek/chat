package lexek.wschat.chat;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.chat.model.User;
import lexek.wschat.chat.processing.HandlerInvoker;
import lexek.wschat.services.AbstractService;
import lexek.wschat.util.LoggingExceptionHandler;

import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DefaultMessageReactor extends AbstractService implements MessageReactor, EventHandler<DefaultMessageReactor.InboundMessageEvent> {
    private final EnumSet<MessageType> availableForBanned = EnumSet.of(
        MessageType.PART,
        MessageType.PING,
        MessageType.JOIN);
    private final HandlerInvoker handlerInvoker;
    private final Disruptor<InboundMessageEvent> disruptor;
    private final RingBuffer<InboundMessageEvent> ringBuffer;
    private final Timer timer = new Timer();

    public DefaultMessageReactor(HandlerInvoker handlerInvoker) {
        super("messageReactor");
        this.handlerInvoker = handlerInvoker;
        EventFactory<InboundMessageEvent> eventFactory = InboundMessageEvent::new;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("MESSAGE_REACTOR_%d").build();
        this.disruptor = new Disruptor<>(
            eventFactory,
            64,
            Executors.newSingleThreadExecutor(threadFactory),
            ProducerType.MULTI,
            new BlockingWaitStrategy()
        );
        this.ringBuffer = this.disruptor.getRingBuffer();
        this.disruptor.handleExceptionsWith(new LoggingExceptionHandler());
        this.disruptor.handleEventsWith(this);
    }

    @Override
    public void processMessage(final Connection connection, final Message message) {
        if (message.getType() == null) {
            logger.debug("{} | Malformed message.", message);
            return;
        }
        long sequence = ringBuffer.next();
        InboundMessageEvent event = ringBuffer.get(sequence);
        event.setConnection(connection);
        event.setMessage(message);
        ringBuffer.publish(sequence);
    }

    private void process(Connection connection, Message message) {
        User user = connection.getUser();

        if (user.isBanned() && !availableForBanned.contains(message.getType())) {
            connection.send(Message.errorMessage("BAN"));
            return;
        }

        handlerInvoker.handle(connection, message);
    }

    @Override
    protected void start0() {
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
        metricRegistry.register(this.getName() + ".events", timer);
    }

    @Override
    public void onEvent(InboundMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        Timer.Context timerContext = timer.time();
        process(event.connection, event.message);
        timerContext.stop();
    }

    public static class InboundMessageEvent {
        private Connection connection;
        private Message message;

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "InboundMessageEvent{" +
                "connection=" + connection +
                ", message=" + message +
                '}';
        }
    }
}