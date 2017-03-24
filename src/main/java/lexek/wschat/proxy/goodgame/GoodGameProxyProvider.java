package lexek.wschat.proxy.goodgame;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.msg.DefaultMessageProcessingService;
import lexek.wschat.chat.msg.EmoticonMessageProcessor;
import lexek.wschat.proxy.*;
import lexek.wschat.services.NotificationService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GoodGameProxyProvider extends ProxyProvider {
    private final Logger logger = LoggerFactory.getLogger(GoodGameProxyProvider.class);
    private final NotificationService notificationService;
    private final ProxyAuthService proxyAuthService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messsageId;
    private final GoodGameApiClient apiClient;
    private final DefaultMessageProcessingService messageProcessingService;

    @Inject
    public GoodGameProxyProvider(
        NotificationService notificationService,
        ProxyAuthService proxyAuthService,
        @Named("proxyEventLoopGroup") EventLoopGroup eventLoopGroup,
        MessageBroadcaster messageBroadcaster,
        @Named("messageId") AtomicLong messsageId,
        GoodGameApiClient apiClient,
        ProxyEmoticonProviderFactory proxyEmoticonProviderFactory
    ) {
        super("goodgame", true, false, false, true, ImmutableSet.of("goodgame"), EnumSet.of(ModerationOperation.BAN));
        this.notificationService = notificationService;
        this.proxyAuthService = proxyAuthService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messsageId = messsageId;
        this.apiClient = apiClient;
        this.messageProcessingService = new DefaultMessageProcessingService();
        this.messageProcessingService.addProcessor(new EmoticonMessageProcessor(
            proxyEmoticonProviderFactory.getProvider(this.getName()),
            "/emoticons/goodgame"
        ));
    }

    @Override
    public Proxy newProxy(ProxyDescriptor descriptor) {
        return new GoodGameChatProxy(
            descriptor,
            messageProcessingService,
            notificationService,
            messageBroadcaster,
            eventLoopGroup,
            messsageId,
            apiClient,
            proxyAuthService
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        try {
            return apiClient.getChannelId(remoteRoom) != null;
        } catch (Exception e) {
            logger.warn("unable to get channel id", e);
            return false;
        }
    }

    @Override
    public List<ProxyEmoticonDescriptor> fetchEmoticonDescriptors() throws Exception {
        return apiClient.getEmoticons();
    }
}
