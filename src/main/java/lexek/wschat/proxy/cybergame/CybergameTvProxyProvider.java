package lexek.wschat.proxy.cybergame;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyEmoticonDescriptor;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.services.NotificationService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CybergameTvProxyProvider extends ProxyProvider {
    private final CybergameTvApiClient apiClient;
    private final NotificationService notificationService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;

    @Inject
    public CybergameTvProxyProvider(
        CybergameTvApiClient apiClient,
        NotificationService notificationService,
        MessageBroadcaster messageBroadcaster,
        @Named("proxyEventLoopGroup") EventLoopGroup eventLoopGroup,
        @Named("messageId") AtomicLong messageId
    ) {
        super("cybergame", false, false, false, true, ImmutableSet.of(), EnumSet.noneOf(ModerationOperation.class));
        this.apiClient = apiClient;
        this.notificationService = notificationService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, Long proxyAuthId, boolean outbound) {
        return new CybergameTvChatProxy(
            apiClient, notificationService, messageBroadcaster, eventLoopGroup, messageId, this, room, remoteRoom, id
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        try {
            return apiClient.getChannelId(remoteRoom) != null;
        } catch (IOException e) {
            throw new InternalErrorException(e);
        }
    }

    @Override
    public List<ProxyEmoticonDescriptor> fetchEmoticonDescriptors() throws Exception {
        return apiClient.getEmoticons();
    }
}
