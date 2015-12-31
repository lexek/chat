package lexek.wschat.proxy;

import lexek.wschat.services.NotificationService;

public abstract class AbstractProxy implements Proxy {
    private final NotificationService notificationService;
    private final long id;
    private final ProxyProvider provider;
    private final String remoteRoom;
    private volatile ProxyState state = ProxyState.NEW;
    private volatile String lastError = null;

    protected AbstractProxy(NotificationService notificationService, long id, ProxyProvider provider, String remoteRoom) {
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
        this.state = ProxyState.RUNNING;
        connect();
    }

    @Override
    final public void stop() {
        this.state = ProxyState.STOPPED;
        disconnect();
    }

    protected void failed(String message) {
        this.state = ProxyState.FAILED;
        this.lastError = message;
        this.notificationService.notifyAdmins(
            "Proxy failed " + provider.getName(),
            String.format("Proxy %s/%s(%d) failed: %s", provider.getName(), remoteRoom, id, message),
            true
        );
    }

    protected abstract void connect();

    protected abstract void disconnect();
}
