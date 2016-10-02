package lexek.wschat.proxy.beam;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.EventLoopGroup;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.services.NotificationService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BeamProxyProvider extends ProxyProvider {
    private final NotificationService notificationService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final BeamDataProvider beamDataProvider;

    @Inject
    public BeamProxyProvider(
        NotificationService notificationService,
        MessageBroadcaster messageBroadcaster,
        @Named("proxyEventLoopGroup") EventLoopGroup eventLoopGroup,
        @Named("messageId") AtomicLong messageId,
        BeamDataProvider beamDataProvider
    ) {
        super("beam", false, false, false, ImmutableSet.of(), EnumSet.noneOf(ModerationOperation.class));
        this.notificationService = notificationService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.beamDataProvider = beamDataProvider;
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, Long proxyAuthId, boolean outbound) {
        return new BeamChatProxy(
            beamDataProvider, notificationService, messageBroadcaster, eventLoopGroup, eventLoopGroup, messageId,
            this, room, remoteRoom, id
        );
    }

    @Override
    public boolean validateRemoteRoom(String remoteRoom) {
        try {
            return beamDataProvider.getId(remoteRoom) >= 0;
        } catch (Exception e) {
            return false;
        }
    }
}
