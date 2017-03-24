package lexek.wschat.proxy.streamlabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.MessageProperty;
import lexek.wschat.chat.model.MessageType;
import lexek.wschat.proxy.AbstractProxy;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.proxy.ProxyDescriptor;
import lexek.wschat.proxy.ProxyTokenException;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StreamLabsProxy extends AbstractProxy {
    private final RequestConfig requestConfig = RequestConfig
        .custom()
        .setConnectTimeout(10000)
        .setSocketTimeout(10000)
        .build();
    private final ScheduledExecutorService scheduler;
    private final ProxyAuthService proxyAuthService;
    private final HttpClient httpClient;
    private final MessageBroadcaster messageBroadcaster;
    private volatile ScheduledFuture scheduledFuture = null;
    private volatile Long lastId = null;
    private final long since = (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2)) / 1000;

    public StreamLabsProxy(
        ProxyDescriptor descriptor,
        ScheduledExecutorService scheduler,
        NotificationService notificationService,
        ProxyAuthService proxyAuthService,
        HttpClient httpClient,
        MessageBroadcaster messageBroadcaster
    ) {
        super(scheduler, notificationService, descriptor);
        this.scheduler = scheduler;
        this.proxyAuthService = proxyAuthService;
        this.httpClient = httpClient;
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    protected void init() throws Exception {
        //ignore
    }

    @Override
    protected void connect() {
        this.scheduledFuture = scheduler.scheduleAtFixedRate(new Worker(), 0, 20, TimeUnit.SECONDS);
        started();
    }

    @Override
    protected void disconnect() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);
        }
    }

    private Message composeMessage(String name, String message, String amount, String currency) {
        return new Message(ImmutableMap.<MessageProperty, Object>builder()
            .put(MessageProperty.TYPE, MessageType.DONATION)
            .put(MessageProperty.ROOM, descriptor.getRoom().getName())
            .put(MessageProperty.NAME, name)
            .put(MessageProperty.TEXT, message)
            .put(StreamLabsProxyProvider.AMOUNT_PROPERTY, amount)
            .put(StreamLabsProxyProvider.CURRENCY_PROPERTY, currency)
            .build()
        );
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            try {
                URIBuilder uriBuilder = new URIBuilder("https://streamlabs.com/api/v1.0/donations");
                uriBuilder.addParameter("access_token", proxyAuthService.getToken(descriptor.getAuthId().get()));
                if (lastId != null) {
                    uriBuilder.addParameter("after", lastId.toString());
                }
                HttpGet httpGet = new HttpGet(uriBuilder.build());
                httpGet.setConfig(requestConfig);
                JsonNode rootNode = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
                for (JsonNode node : rootNode.get("data")) {
                    long id = Long.parseLong(node.get("donation_id").asText());
                    boolean pass = true;
                    if (lastId == null || id > lastId) {
                        pass = false;
                        long createdAt = Long.parseLong(node.get("created_at").asText());
                        if (createdAt < since) {
                            pass = true;
                        }
                        lastId = id;
                    }
                    if (!pass) {
                        messageBroadcaster.submitMessage(composeMessage(
                            node.get("name").asText(),
                            node.get("message").asText(),
                            node.get("amount").asText(),
                            node.get("currency").asText()
                        ), descriptor.getRoom().FILTER);
                    }
                }
            } catch (ProxyTokenException e) {
                logger.error("exception", e);
                fatalError(e.getMessage());
            } catch (IOException e) {
                logger.warn("exception", e);
                minorFail(e.getMessage());
            } catch (Exception e) {
                logger.error("exception", e);
                fail(e.getMessage());
            }
        }
    }
}
