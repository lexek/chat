package lexek.wschat.proxy.sc2tv;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.msg.DefaultMessageProcessingService;
import lexek.wschat.chat.msg.MessageProcessingService;
import lexek.wschat.chat.msg.MessageProcessor;
import lexek.wschat.chat.msg.UrlMessageProcessor;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
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
        @Named("proxyEventLoopGroup") EventLoopGroup eventLoopGroup,
        MessageBroadcaster messageBroadcaster,
        @Named("messageId") AtomicLong messageId
    ) {
        super("peka2tv", false, false, false, false, ImmutableSet.of(), EnumSet.noneOf(ModerationOperation.class));
        this.apiClient = apiClient;
        this.notificationService = notificationService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        List<MessageProcessor> processors = new ArrayList<>();
        processors.add(new UrlMessageProcessor());
        this.messageProcessingService = new DefaultMessageProcessingService(processors);

    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, Long proxyAuthId, boolean outbound) {
        return new Sc2tvChatProxy(
            notificationService,
            remoteRoom,
            messageBroadcaster,
            eventLoopGroup,
            messageId,
            room,
            id,
            this,
            apiClient,
            messageProcessingService
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        try {
            return apiClient.getStreamId(remoteRoom) != null;
        } catch (IOException e) {
            logger.warn("error while checking remote room", e);
            return false;
        }
    }
}
