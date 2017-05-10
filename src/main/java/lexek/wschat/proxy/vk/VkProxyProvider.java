package lexek.wschat.proxy.vk;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.proxy.*;
import lexek.wschat.services.NotificationService;
import org.apache.http.client.HttpClient;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class VkProxyProvider extends ProxyProvider {
    public static final String API_VERSION = "5.63";

    private final ScheduledExecutorService scheduledExecutorService;
    private final NotificationService notificationService;
    private final ProxyAuthService proxyAuthService;
    private final HttpClient httpClient;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final VkApiClient vkApiClient;

    @Inject
    public VkProxyProvider(
        ScheduledExecutorService scheduledExecutorService,
        NotificationService notificationService,
        ProxyAuthService proxyAuthService,
        HttpClient httpClient,
        MessageBroadcaster messageBroadcaster,
        AtomicLong messageId,
        VkApiClient vkApiClient) {
        super("vk", true, false, true, false, ImmutableSet.of("vk"), EnumSet.noneOf(ModerationOperation.class));
        this.scheduledExecutorService = scheduledExecutorService;
        this.notificationService = notificationService;
        this.proxyAuthService = proxyAuthService;
        this.httpClient = httpClient;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.vkApiClient = vkApiClient;
    }

    @Override
    public Proxy newProxy(ProxyDescriptor descriptor) {
        return new VkProxy(descriptor, scheduledExecutorService, notificationService, proxyAuthService, httpClient, messageBroadcaster, messageId, vkApiClient);
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom, Long authId) {
        return true;
    }
}
