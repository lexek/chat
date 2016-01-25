package lexek.wschat.proxy.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpMethod;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.Proxy;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.JsonResponseHandler;
import lexek.wschat.util.OAuthUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.SSLException;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class TwitterProxyProvider extends ProxyProvider {
    private static final int TIMEOUT = 3000;
    private final NotificationService notificationService;
    private final EventLoopGroup eventLoopGroup;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private final HttpClient httpClient;
    private final String consumerKey;
    private final String consumerSecret;

    public TwitterProxyProvider(
        NotificationService notificationService,
        MessageBroadcaster messageBroadcaster,
        EventLoopGroup eventLoopGroup,
        AtomicLong messageId,
        String consumerKey,
        String consumerSecret
    ) {
        super("twitter", true, false, EnumSet.noneOf(ModerationOperation.class));
        this.notificationService = notificationService;
        this.eventLoopGroup = eventLoopGroup;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(2);
        connectionManager.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(TIMEOUT).build());
        this.httpClient = HttpClients
            .custom()
            .setDefaultRequestConfig(RequestConfig
                .custom()
                .setConnectionRequestTimeout(TIMEOUT)
                .setConnectTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .build()
            )
            .setConnectionManager(connectionManager)
            .build();
    }

    @Override
    public Proxy newProxy(long id, Room room, String remoteRoom, String name, String key, boolean outbound) {
        String[] s = key.split(":");
        try {
            return new TwitterProxy(
                notificationService,
                messageBroadcaster,
                eventLoopGroup,
                this,
                id,
                remoteRoom,
                messageId,
                room,
                consumerKey,
                consumerSecret,
                s[0],
                s[1]
            );
        } catch (SSLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean validateCredentials(String name, String tokenPair) {
        String[] s = tokenPair.split(":");
        if (s.length != 2) {
            return false;
        }
        String token = s[0];
        String tokenSecret = s[1];
        String url = "https://api.twitter.com/1.1/account/verify_credentials.json";
        HttpGet request = new HttpGet(url);
        try {
            request.setHeader("Authorization", OAuthUtil.generateAuthorizationHeader(
                consumerKey, consumerSecret, token, tokenSecret, url, HttpMethod.GET, ImmutableMap.<String, String>of()
            ));
            JsonNode root = httpClient.execute(request, JsonResponseHandler.INSTANCE);
            System.out.println(root);
            return name.equalsIgnoreCase(root.get("screen_name").asText());
        } catch (Exception e) {
            return false;
        }
    }
}
