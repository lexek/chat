package lexek.wschat.proxy.youtube;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.proxy.*;
import lexek.wschat.services.NotificationService;
import org.apache.http.client.HttpClient;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class YouTubeProxyProvider extends ProxyProvider {
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private final NotificationService notificationService;
    private final ScheduledExecutorService executorService;
    private final ProxyAuthService proxyAuthService;
    private final HttpClient httpClient;

    @Inject
    public YouTubeProxyProvider(
        @Named("messageId") AtomicLong messageId,
        MessageBroadcaster messageBroadcaster,
        NotificationService notificationService,
        ScheduledExecutorService executorService,
        ProxyAuthService proxyAuthService,
        HttpClient httpClient
    ) {
        super("youtube", true, false, true, false, ImmutableSet.of("google"), EnumSet.noneOf(ModerationOperation.class));
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.notificationService = notificationService;
        this.executorService = executorService;
        this.proxyAuthService = proxyAuthService;
        this.httpClient = httpClient;
    }

    @Override
    public Proxy newProxy(ProxyDescriptor descriptor) {
        return new YouTubeProxy(
            descriptor,
            messageId,
            notificationService,
            executorService,
            httpClient,
            proxyAuthService,
            messageBroadcaster
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        return true;
    }
}
