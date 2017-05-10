package lexek.wschat.proxy.sc2tv;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.msg.*;
import lexek.wschat.proxy.*;
import lexek.wschat.services.NotificationService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class Sc2tvProxyProvider extends ProxyProvider {
    private final Logger logger = LoggerFactory.getLogger(Sc2tvProxyProvider.class);
    private final Peka2TvApiClient apiClient;
    private final NotificationService notificationService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final MessageProcessingService messageProcessingService;

    @Inject
    public Sc2tvProxyProvider(
        Peka2TvApiClient apiClient,
        NotificationService notificationService,
        ProxyEmoticonProviderFactory proxyEmoticonProviderFactory,
        @Named("proxyEventLoopGroup") EventLoopGroup eventLoopGroup,
        MessageBroadcaster messageBroadcaster,
        @Named("messageId") AtomicLong messageId
    ) {
        super("peka2tv", false, false, false, true, ImmutableSet.of(), EnumSet.noneOf(ModerationOperation.class));
        this.apiClient = apiClient;
        this.notificationService = notificationService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        List<MessageProcessor> processors = new ArrayList<>();
        processors.add(new UrlMessageProcessor());
        processors.add(new EmoticonMessageProcessor(
            proxyEmoticonProviderFactory.getProvider(this.getName()),
            "/emoticons/peka2tv"
        ));
        this.messageProcessingService = new DefaultMessageProcessingService(processors);

    }

    @Override
    public Proxy newProxy(ProxyDescriptor descriptor) {
        return new Sc2tvChatProxy(
            descriptor,
            notificationService,
            messageBroadcaster,
            eventLoopGroup,
            messageId,
            apiClient,
            messageProcessingService
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom, Long authId) {
        try {
            return apiClient.getStreamId(remoteRoom) != null;
        } catch (IOException e) {
            logger.warn("error while checking remote room", e);
            return false;
        }
    }

    @Override
    public List<ProxyEmoticonDescriptor> fetchEmoticonDescriptors() throws Exception {
        return apiClient.getEmoticons();
    }
}
