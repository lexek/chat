package lexek.wschat.proxy;

import lexek.wschat.services.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractProxy implements Proxy {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final NotificationService notificationService;
    private final long id;
    private final ProxyProvider provider;
    private final ScheduledExecutorService scheduler;
    private final String remoteRoom;
    private int failsInRow = 0;
    private volatile ProxyState state = ProxyState.NEW;
    private volatile String lastError = null;
    private volatile ScheduledFuture checkFuture = null;
    private volatile ScheduledFuture reconnectFuture = null;

    protected AbstractProxy(
        ScheduledExecutorService scheduler,
        NotificationService notificationService,
        ProxyProvider provider,
        long id,
        String remoteRoom
    ) {
        this.scheduler = scheduler;
        this.notificationService = notificationService;
        this.id = id;
        this.provider = provider;
        this.remoteRoom = remoteRoom;
    }

    @Override
    final public long id() {
        return id;
    }

    @Override
    final public ProxyProvider provider() {
        return provider;
    }

    @Override
    final public String remoteRoom() {
        return remoteRoom;
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
    final public synchronized void start() {
        logger.info("proxy {}/{} starting", provider.getName(), remoteRoom);
        if (checkFuture != null) {
            checkFuture.cancel(false);
            checkFuture = null;
        }
        state = ProxyState.STARTING;
        connect();
        checkFuture = scheduler.schedule(this::checkIfRunning, 1, TimeUnit.MINUTES);
    }

    @Override
    final public synchronized void stop() {
        logger.info("proxy {}/{} stopped", provider.getName(), remoteRoom);
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
        logger.info("proxy {}/{} started", provider.getName(), remoteRoom);
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
        logger.warn("proxy {}/{} failed: {} (minor: {})", provider.getName(), remoteRoom, message, !notify);
        state = ProxyState.RECONNECTING;
        lastError = message;
        disconnect();
        if (notify) {
            notificationService.notifySuperAdmins(
                "Proxy failed " + provider.getName(),
                String.format("Proxy %s/%s(%d) failed: %s", provider.getName(), remoteRoom, id, message),
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

    protected abstract void connect();

    protected abstract void disconnect();
}
