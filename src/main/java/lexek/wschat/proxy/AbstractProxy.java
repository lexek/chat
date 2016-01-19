package lexek.wschat.proxy;

import lexek.wschat.services.NotificationService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractProxy implements Proxy {
    private final NotificationService notificationService;
    private final long id;
    private final ProxyProvider provider;
    private final ScheduledExecutorService scheduler;
    private final String remoteRoom;
    private int failsInRow = 0;
    private volatile ProxyState state = ProxyState.NEW;
    private volatile String lastError = null;

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
        return this.id;
    }

    @Override
    final public ProxyProvider provider() {
        return this.provider;
    }

    @Override
    final public String remoteRoom() {
        return this.remoteRoom;
    }

    @Override
    final public ProxyState state() {
        return this.state;
    }

    @Override
    final public String lastError() {
        return this.lastError;
    }

    @Override
    final public void start() {
        this.state = ProxyState.STARTING;
        connect();
        this.scheduler.schedule(this::checkIfRunning, 1, TimeUnit.MINUTES);
    }

    @Override
    final public void stop() {
        this.state = ProxyState.STOPPED;
        disconnect();
    }

    protected void started() {
        this.state = ProxyState.RUNNING;
        this.failsInRow = 0;
    }

    protected void failed(String message) {
        this.failsInRow++;
        this.state = ProxyState.FAILED;
        this.lastError = message;
        disconnect();
        this.notificationService.notifyAdmins(
            "Proxy failed " + provider.getName(),
            String.format("Proxy %s/%s(%d) failed: %s", provider.getName(), remoteRoom, id, message),
            true
        );
        long reconnectIn = failsInRow <= 5 ? Math.round(Math.pow(2, this.failsInRow)) : 32;
        this.scheduler.schedule(this::start, reconnectIn, TimeUnit.MINUTES);
    }

    private void checkIfRunning() {
        if (this.state == ProxyState.STARTING) {
            this.failed("didn't start within given time");
        }
    }

    protected abstract void connect();

    protected abstract void disconnect();
}
