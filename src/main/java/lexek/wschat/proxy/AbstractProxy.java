package lexek.wschat.proxy;

import lexek.wschat.chat.model.Message;
import lexek.wschat.services.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractProxy implements Proxy {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final ProxyDescriptor descriptor;

    private final NotificationService notificationService;
    private final ScheduledExecutorService scheduler;
    private int failsInRow = 0;
    private volatile ProxyState state = ProxyState.NEW;
    private volatile String lastError = null;
    private volatile ScheduledFuture checkFuture = null;
    private volatile ScheduledFuture reconnectFuture = null;
    private volatile boolean initialized = false;

    protected AbstractProxy(
        ScheduledExecutorService scheduler,
        NotificationService notificationService,
        ProxyDescriptor descriptor
    ) {
        this.scheduler = scheduler;
        this.notificationService = notificationService;
        this.descriptor = descriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractProxy that = (AbstractProxy) o;
        return descriptor.getId() == that.descriptor.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptor.getId());
    }

    @Override
    final public long id() {
        return descriptor.getId();
    }

    @Override
    final public ProxyProvider provider() {
        return descriptor.getProvider();
    }

    @Override
    public String remoteRoom() {
        return descriptor.getRemoteRoom();
    }

    @Override
    final public boolean outboundEnabled() {
        return descriptor.hasFeature(ProxyFeature.OUTBOUND);
    }

    @Override
    final public boolean moderationEnabled() {
        return descriptor.hasFeature(ProxyFeature.MODERATION);
    }

    @Override
    final public ProxyState state() {
        return state;
    }

    @Override
    final public String lastError() {
        return lastError;
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMessage(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    final public synchronized void start() {
        if (!initialized) {
            try {
                init();
                initialized = true;
            } catch (Exception e) {
                logger.error("failed to initialize proxy {}/{}", provider().getName(), remoteRoom());
                fail(e.getMessage(), true, true);
            }
        }
        logger.info("proxy {}/{} starting", provider().getName(), remoteRoom());
        if (checkFuture != null) {
            checkFuture.cancel(false);
            checkFuture = null;
        }
        state = ProxyState.STARTING;
        try {
            connect();
            checkFuture = scheduler.schedule(this::checkIfRunning, 1, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error("failed to connect proxy {}/{}", provider().getName(), remoteRoom());
            fail(e.getMessage());
        }
    }

    @Override
    final public synchronized void stop() {
        logger.info("proxy {}/{} stopped", provider().getName(), remoteRoom());
        if (checkFuture != null) {
            checkFuture.cancel(false);
            checkFuture = null;
        }
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }
        state = ProxyState.STOPPED;
        disconnect();
    }

    protected void started() {
        logger.info("proxy {}/{} started", provider().getName(), remoteRoom());
        if (checkFuture != null) {
            checkFuture.cancel(false);
            checkFuture = null;
        }
        state = ProxyState.RUNNING;
        lastError = null;
        failsInRow = 0;
    }

    protected void minorFail(String message) {
        fail(message, false, false);
    }

    protected void fail(String message) {
        fail(message, false, true);
    }

    protected void fatalError(String message) {
        fail(message, true, true);
    }

    private void fail(String message, boolean fatal, boolean notify) {
        logger.warn("proxy {}/{} failed: {} (minor: {})", provider().getName(), remoteRoom(), message, !notify);
        state = ProxyState.RECONNECTING;
        lastError = message;
        disconnect();
        if (notify) {
            notificationService.notifySuperAdmins(
                "Proxy failed " + provider().getName(),
                String.format("Proxy %s/%s(%d) failed: %s", provider().getName(), remoteRoom(), id(), message),
                true
            );
        }
        //if the error is fatal we don't need to reconnect automatically
        if (failsInRow == 0) {
            //reconnect right away on first fail
            start();
        } else {
            long reconnectIn = failsInRow <= 5 ? Math.round(Math.pow(2, failsInRow)) : 32;
            if (fatal) {
                reconnectIn = 30;
            }
            reconnectFuture = scheduler.schedule(this::start, reconnectIn, TimeUnit.MINUTES);
        }
        failsInRow++;
    }

    private void checkIfRunning() {
        if (state == ProxyState.STARTING) {
            fail("didn't start within given time");
        }
        checkFuture = null;
    }

    protected abstract void connect() throws Exception;

    protected abstract void disconnect();

    protected abstract void init() throws Exception;
}
