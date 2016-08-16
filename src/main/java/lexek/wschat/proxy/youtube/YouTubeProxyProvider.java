package lexek.wschat.proxy.youtube;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.security.social.SocialProfile;
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
        super("youtube", true, false, true, ImmutableSet.of("google"), EnumSet.of(ModerationOperation.BAN));
        this.messageId = messageId;
        this.messageBroadcaster = messageBroadcaster;
        this.notificationService = notificationService;
        this.executorService = executorService;
        this.proxyAuthService = proxyAuthService;
        this.httpClient = httpClient;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, Long proxyAuthId, boolean outbound) {
        SocialProfile socialProfile = proxyAuthService.getProfile(proxyAuthId);
        return new YouTubeProxy(
            messageId,
            notificationService,
            executorService,
            httpClient,
            proxyAuthService,
            this,
            proxyAuthId,
            id,
            messageBroadcaster,
            room,
            socialProfile.getName()
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        return true;
    }
}
