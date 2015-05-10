package lexek.wschat.chat;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lexek.wschat.services.AbstractService;
import lexek.wschat.util.LoggingExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DefaultMessageReactor extends AbstractService implements MessageReactor, EventHandler<DefaultMessageReactor.InboundMessageEvent> {
    private final Logger messageLogger = LoggerFactory.getLogger("messages");
    private final Table<MessageType, Integer, MessageHandler> handlers = HashBasedTable.create();
    private final EnumSet<MessageType> availableForBanned = EnumSet.of(
            MessageType.PART,
            MessageType.PING,
            MessageType.JOIN);
    private final EnumSet<MessageType> meteredTypes = EnumSet.of(MessageType.MSG, MessageType.ME);
    private final Meter meter;
    private final Disruptor<InboundMessageEvent> disruptor;
    private final RingBuffer<InboundMessageEvent> ringBuffer;

    public DefaultMessageReactor(MetricRegistry metricRegistry) {
        super("messageReactor", ImmutableList.<String>of());
        meter = metricRegistry.meter("activity");
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
    public void registerHandler(@NotNull MessageHandler handler) {
        handlers.put(handler.getType(), handler.getArgCount(), handler);
    }

    @Override
    public void processMessage(final Connection connection, final InboundMessage message) {
        if (message.getType() == null || message.getArgs() == null) {
            logger.debug("{} | Malformed message.", message);
            return;
        }
        long sequence = ringBuffer.next();
        InboundMessageEvent event = ringBuffer.get(sequence);
        event.setConnection(connection);
        event.setMessage(message);
        ringBuffer.publish(sequence);
    }

    private void process(Connection connection, InboundMessage message) {
        long t = System.currentTimeMillis();
        User user = connection.getUser();

        if (user.isBanned() && !availableForBanned.contains(message.getType())) {
            connection.send(Message.errorMessage("BAN"));
            return;
        }

        MessageHandler handler = handlers.get(message.getType(), message.getArgCount());
        if (handler == null) {
            connection.send(Message.errorMessage("UNKNOWN_COMMAND"));
            messageLogger.info("{}[{}] HANDLER: NULL; MSG: {}", user.getName(), connection.getIp(),
                    message.getType().toString() + message.getArgs());
        } else {
            int interval = user.getRole().getMessageTimeInterval();
            long timeFromLastMessage = System.currentTimeMillis() - user.getLastMessage();
            if (!handler.isNeedsInterval() || (interval == 0) || (timeFromLastMessage > interval)) {
                handleMessage(handler, connection, user, message);
                if (handler.isNeedsInterval()) {
                    user.setLastMessage(t);
                }
            } else {
                connection.send(Message.errorMessage("TOO_FAST"));
            }
        }
    }

    private void handleMessage(MessageHandler handler, Connection connection, User user, InboundMessage message) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }

        if (meteredTypes.contains(message.getType())) {
            meter.mark();
        }

        if (user.getRole().compareTo(handler.getRole()) >= 0) {
            handler.handle(message.getArgs(), connection);
            if (handler.isNeedsLogging()) {
                messageLogger.info("{}[{}] HANDLER: {}; MSG: {} {}", user.getName(), connection.getIp(),
                        handler.getClass().getSimpleName(), message.getType(), message.getArgs());
            } else {
                messageLogger.debug("{}[{}] HANDLER: {}; MSG: {} {}", user.getName(), connection.getIp(),
                        handler.getClass().getSimpleName(), message.getType(), message.getArgs());
            }
        } else {
            connection.send(Message.errorMessage("NOT_AUTHORIZED"));
        }
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
    public void onEvent(InboundMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        process(event.connection, event.message);
    }

    @Override
    public void performAction(String action) {

    }

    public static class InboundMessageEvent {
        private Connection connection;
        private InboundMessage message;

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

        public InboundMessage getMessage() {
            return message;
        }

        public void setMessage(InboundMessage message) {
            this.message = message;
        }
    }
}