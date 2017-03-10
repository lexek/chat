package lexek.wschat.chat;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lexek.wschat.chat.filters.BroadcastFilter;
import lexek.wschat.chat.model.Message;
import lexek.wschat.services.managed.AbstractManagedService;
import lexek.wschat.services.managed.InitStage;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.TopicProcessor;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Service
public class MessageBroadcaster extends AbstractManagedService {
    private final TopicProcessor<MessageEvent> publisher;

    @Inject
    public MessageBroadcaster() {
        super("messageBroadcaster", InitStage.SERVICES);
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("MESSAGE_BROADCASTER_%d").build();
        publisher = TopicProcessor.share(
            Executors.newCachedThreadPool(threadFactory),
            64,
            false
        );
    }

    public void init(Iterable<MessageEventHandler> handlers) {
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
        publisher.onNext(new MessageEvent(message, filter));
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
        publisher.subscribe(new BaseSubscriber<MessageEvent>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(1);
            }

            @Override
            protected void hookOnNext(MessageEvent event) {
                try {
                    consumer.onEvent(event.getMessage(), event.getBroadcastFilter());
                } catch (Exception e) {
                    logger.error("uncaught exception", e);
                }
                request(1);
            }
        });
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        publisher.shutdown();
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
        metricRegistry.register(this.getName() + ".queue.remainingCapacity", (Gauge<Long>) publisher::getCapacity);
        metricRegistry.register(this.getName() + ".queue.bufferSize", (Gauge<Long>) publisher::getAvailableCapacity);
    }
}
