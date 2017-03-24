package lexek.wschat.proxy.streamlabs;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.proxy.*;
import lexek.wschat.services.NotificationService;
import org.apache.http.client.HttpClient;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;

@Service
public class StreamLabsProxyProvider extends ProxyProvider {
    public static final MessageProperty<String> CURRENCY_PROPERTY = MessageProperty.valueOf("currency");
    public static final MessageProperty<String> AMOUNT_PROPERTY = MessageProperty.valueOf("amount");

    private final ScheduledExecutorService scheduledExecutorService;
    private final NotificationService notificationService;
    private final ProxyAuthService proxyAuthService;
    private final HttpClient httpClient;
    private final MessageBroadcaster messageBroadcaster;

    @Inject
    public StreamLabsProxyProvider(
        ScheduledExecutorService scheduledExecutorService,
        NotificationService notificationService,
        ProxyAuthService proxyAuthService,
        HttpClient httpClient,
        MessageBroadcaster messageBroadcaster
    ) {
        super("streamlabs", true, false, true, false, ImmutableSet.of("streamlabs"), EnumSet.noneOf(ModerationOperation.class));
        this.scheduledExecutorService = scheduledExecutorService;
        this.notificationService = notificationService;
        this.proxyAuthService = proxyAuthService;
        this.httpClient = httpClient;
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    public Proxy newProxy(ProxyDescriptor descriptor) {
        return new StreamLabsProxy(
            descriptor, scheduledExecutorService, notificationService, proxyAuthService, httpClient, messageBroadcaster
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        return true;
    }
}
