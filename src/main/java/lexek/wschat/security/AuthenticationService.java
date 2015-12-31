package lexek.wschat.security;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.evt.EventDispatcher;
import lexek.wschat.chat.model.ConnectionState;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.model.UserAuthDto;
import lexek.wschat.services.AbstractService;
import lexek.wschat.services.UserService;
import lexek.wschat.util.LoggingExceptionHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class AuthenticationService extends AbstractService {
    private final Disruptor<AuthenticationEvent> disruptor;
    private final RingBuffer<AuthenticationEvent> ringBuffer;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final CaptchaService captchaService;
    private final EventDispatcher eventDispatcher;

    public AuthenticationService(
        AuthenticationManager authenticationManager,
        UserService userService,
        CaptchaService captchaService,
        EventDispatcher eventDispatcher
    ) {
        super("authenticationService");
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.captchaService = captchaService;
        this.eventDispatcher = eventDispatcher;
        EventFactory<AuthenticationEvent> eventFactory = AuthenticationEvent::new;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("AUTHENTICATION_SERVICE_%d").build();
        this.disruptor = new Disruptor<>(
            eventFactory,
            32,
            Executors.newSingleThreadExecutor(threadFactory),
            ProducerType.MULTI,
            new BlockingWaitStrategy()
        );
        this.ringBuffer = this.disruptor.getRingBuffer();
        this.disruptor.handleExceptionsWith(new LoggingExceptionHandler());
        this.disruptor.handleEventsWith(new AuthenticationServiceWorker());
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
    }

    public void invalidate(String sid) {
        authenticationManager.invalidateSession(sid);
    }

    public void authenticateWithSid(Connection connection, String sid, AuthenticationCallback callback) {
        long sequence = ringBuffer.next();
        connection.setState(ConnectionState.AUTHENTICATING);
        AuthenticationEvent event = ringBuffer.get(sequence);
        event.setType(AuthenticationType.SID);
        event.setConnection(connection);
        event.setName(null);
        event.setKey(sid);
        event.setCallback(callback);
        ringBuffer.publish(sequence);
    }

    public void authenticateWithPassword(Connection connection, String u, String pw, AuthenticationCallback callback) {
        long sequence = ringBuffer.next();
        connection.setState(ConnectionState.AUTHENTICATING);
        AuthenticationEvent event = ringBuffer.get(sequence);
        event.setType(AuthenticationType.PASSWORD);
        event.setConnection(connection);
        event.setName(u);
        event.setKey(pw);
        event.setCallback(callback);
        ringBuffer.publish(sequence);
    }

    private enum AuthenticationType {
        PASSWORD,
        SID
    }

    private static class AuthenticationEvent {
        private Connection connection;
        private AuthenticationType type;
        private String name;
        private String key;
        private AuthenticationCallback callback;

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

        public AuthenticationType getType() {
            return type;
        }

        public void setType(AuthenticationType type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public AuthenticationCallback getCallback() {
            return callback;
        }

        public void setCallback(AuthenticationCallback callback) {
            this.callback = callback;
        }
    }

    private class AuthenticationServiceWorker implements EventHandler<AuthenticationEvent> {
        @Override
        public void onEvent(final AuthenticationEvent event, long l, boolean b) throws Exception {
            if (event.connection.getState() == ConnectionState.AUTHENTICATING) {
                UserAuthDto auth = null;
                final String ip = event.getConnection().getIp();
                final AuthenticationCallback callback = event.getCallback();
                final Connection connection = event.getConnection();
                if (event.getType() == AuthenticationType.SID && event.getKey() != null) {
                    auth = authenticationManager.checkFullAuthentication(event.getKey(), ip);
                } else if (event.getType() == AuthenticationType.PASSWORD && event.getKey() != null && event.getName() != null) {
                    if (authenticationManager.failedLoginTries(ip) > 10) {
                        callback.captchaRequired(connection, event.getName(), captchaService.tryAuthorize(() ->
                            finishAuthentication(authenticationManager.fastAuth(event.getName(), event.getKey(), ip),
                                connection, callback)));
                        return;
                    } else {
                        auth = authenticationManager.fastAuth(event.getName(), event.getKey(), ip);
                    }
                }
                finishAuthentication(auth, connection, callback);
            }
        }

        private void finishAuthentication(UserAuthDto auth, Connection connection, AuthenticationCallback callback) {
            if (auth != null && auth.getUser() != null) {
                connection.setUser(userService.cache(auth.getUser()));
            } else {
                connection.setUser(User.UNAUTHENTICATED_USER);
            }
            connection.send(Message.authCompleteMessage(connection.getUser().getWrappedObject()));
            callback.authenticationComplete(connection);
            eventDispatcher.connected(connection);
        }
    }
}
