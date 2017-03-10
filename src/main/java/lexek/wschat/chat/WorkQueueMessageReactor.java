package lexek.wschat.chat;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.chat.model.User;
import lexek.wschat.chat.processing.HandlerInvoker;
import lexek.wschat.services.managed.AbstractManagedService;
import lexek.wschat.services.managed.InitStage;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.WorkQueueProcessor;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Service
public class WorkQueueMessageReactor extends AbstractManagedService implements MessageReactor {
    private final EnumSet<MessageType> availableForBanned = EnumSet.of(
        MessageType.PART,
        MessageType.PING,
        MessageType.JOIN);
    private final HandlerInvoker handlerInvoker;
    private final WorkQueueProcessor<InboundMessageEvent> processor;
    private final Timer timer = new Timer();

    @Inject
    public WorkQueueMessageReactor(HandlerInvoker handlerInvoker) {
        super("messageReactor", InitStage.CORE);
        this.handlerInvoker = handlerInvoker;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("MESSAGE_REACTOR_%d").build();
        this.processor = WorkQueueProcessor.share(
            Executors.newCachedThreadPool(threadFactory),
            64,
            false
        );
    }

    @Override
    public void processMessage(InboundMessageEvent event) {
        if (event.getMessage().getType() == null) {
            logger.debug("{} | Malformed message.", event.getMessage());
            return;
        }
        processor.onNext(event);
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
    public void start() {
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
            processor.subscribe(new EventSubscriber(i));
        }
    }

    @Override
    public void stop() {
        processor.shutdown();
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
        metricRegistry.register(this.getName() + ".queue.remainingCapacity", (Gauge<Long>) processor::getCapacity);
        metricRegistry.register(this.getName() + ".queue.bufferSize", (Gauge<Long>) processor::getAvailableCapacity);
        metricRegistry.register(this.getName() + ".events", timer);
    }

    private class EventSubscriber extends BaseSubscriber<InboundMessageEvent> {
        private final int workerId;

        private EventSubscriber(int workerId) {
            this.workerId = workerId;
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            request(1);
        }

        @Override
        protected void hookOnNext(InboundMessageEvent event) {
            Timer.Context timerContext = timer.time();
            try {
                process(event.getConnection(), event.getMessage());
            } catch (Throwable t) {
                logger.error("uncaught exception wtf", t);
            }
            timerContext.stop();
            request(1);
        }

        @Override
        protected void hookOnError(Throwable t) {
            logger.error("worker {} is dead", workerId, t);
        }
    }
}