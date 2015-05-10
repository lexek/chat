package lexek.wschat.proxy;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.RoomManager;
import lexek.wschat.proxy.cybergame.CybergameTvChannelInformationProvider;
import lexek.wschat.proxy.cybergame.CybergameTvChatProxy;
import lexek.wschat.proxy.twitch.TwitchTvChannelInformationProvider;
import lexek.wschat.proxy.twitch.TwitchTvChatProxy;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.services.Service;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class ChatProxyFactory {
    private final ConnectionManager connectionManager;
    private final AtomicLong messageId;
    private final AuthenticationManager authenticationManager;
    private final RoomManager roomManager;
    private final MessageBroadcaster messageBroadcaster;
    private final StreamStatsAggregator streamStatsAggregator = new StreamStatsAggregator();
    private final EventLoopGroup eventLoopGroup;
    private final CloseableHttpClient httpClient;

    public ChatProxyFactory(ConnectionManager connectionManager,
                            AtomicLong messageId,
                            AuthenticationManager authenticationManager,
                            RoomManager roomManager,
                            MessageBroadcaster messageBroadcaster,
                            MetricRegistry metricRegistry) {
        this.connectionManager = connectionManager;
        this.messageId = messageId;
        this.authenticationManager = authenticationManager;
        this.roomManager = roomManager;
        this.messageBroadcaster = messageBroadcaster;

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("PROXY_THREAD_%d").build();
        if (Epoll.isAvailable()) {
            eventLoopGroup = new EpollEventLoopGroup(1, threadFactory);
        } else {
            eventLoopGroup = new NioEventLoopGroup(1, threadFactory);
        }

        metricRegistry.register("viewers", streamStatsAggregator);
        PoolingHttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager();
        httpClientConnectionManager.setMaxTotal(5);
        httpClientConnectionManager.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(true).build());
        httpClient = HttpClients.custom().setConnectionManager(httpClientConnectionManager).build();
    }

    public Service newInstance(ProxyConfiguration proxyConfiguration) {
        Room targetRoom = roomManager.getRoomInstance("#main");
        switch (proxyConfiguration.getType()) {
            case "twitch": {
                TwitchTvChatProxy result = new TwitchTvChatProxy(proxyConfiguration.getChannel(),
                        connectionManager,
                        messageId,
                        messageBroadcaster,
                        authenticationManager,
                        targetRoom,
                        eventLoopGroup);
                streamStatsAggregator.addPlatform(new TwitchTvChannelInformationProvider(
                        proxyConfiguration.getChannel(),
                        httpClient
                ), proxyConfiguration.isUseTitle());
                return result;
            }
            case "cybergame": {
                CybergameTvChatProxy result = new CybergameTvChatProxy(eventLoopGroup,
                        proxyConfiguration.getChannel(),
                        messageBroadcaster,
                        messageId,
                        targetRoom);
                streamStatsAggregator.addPlatform(new CybergameTvChannelInformationProvider(
                        httpClient, proxyConfiguration.getChannel()
                ), false);
                return result;
            }
            case "sc2tv": {
                return new Sc2tvChatProxy(proxyConfiguration.getChannel(),
                        messageBroadcaster,
                        eventLoopGroup,
                        messageId,
                        targetRoom);
            }
            default:
                throw new IllegalArgumentException("Unknown proxy name");
        }
    }
}
