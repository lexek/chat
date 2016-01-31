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
    final public void start() {
        logger.info("proxy {}/{} starting", provider.getName(), remoteRoom);
        state = ProxyState.STARTING;
        connect();
        checkFuture = scheduler.schedule(this::checkIfRunning, 1, TimeUnit.MINUTES);
    }

    @Override
    final public void stop() {
        logger.info("proxy {}/{} stopped", provider.getName(), remoteRoom);
        if (checkFuture != null) {
            checkFuture.cancel(false);
            checkFuture = null;
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
        failsInRow = 0;
    }

    protected void fail(String message) {
        fail(message, false);
    }

    protected void minorFail(String message) {
        fail(message, true);
    }

    protected void fail(String message, boolean minor) {
        logger.warn("proxy {}/{} failed: {} (minor: {})", provider.getName(), remoteRoom, message, minor);
        state = ProxyState.FAILED;
        lastError = message;
        disconnect();
        notificationService.notifyAdmins(
            "Proxy failed " + provider.getName(),
            String.format("Proxy %s/%s(%d) failed: %s", provider.getName(), remoteRoom, id, message),
            !minor
        );
        if (failsInRow == 0) {
            //reconnect right away on first fail
            start();
        } else {
            if (minor) {
                //with minor issue we shouldn't increase reconnection interval
                scheduler.schedule(this::start, 1, TimeUnit.MINUTES);
            } else {
                long reconnectIn = failsInRow <= 5 ? Math.round(Math.pow(2, failsInRow)) : 32;
                scheduler.schedule(this::start, reconnectIn, TimeUnit.MINUTES);
            }
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
