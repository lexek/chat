package lexek.wschat.proxy.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.google.common.net.UrlEscapers;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.proxy.AbstractProxy;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.Colors;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class YouTubeProxy extends AbstractProxy {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong messageId;
    private final MessageBroadcaster messageBroadcaster;
    private final ScheduledExecutorService executorService;
    private final HttpClient httpClient;
    private final ProxyAuthService proxyAuthService;
    private final Long authId;
    private final Room room;
    private final String ownerName;
    private long lastRead = System.currentTimeMillis();
    private int pollInterval = 5000;
    private String lastPageToken = null;
    private String liveChatId = null;

    //todo: custom response handler
    public YouTubeProxy(
        AtomicLong messageId, NotificationService notificationService,
        ScheduledExecutorService executorService,
        HttpClient httpClient,
        ProxyAuthService proxyAuthService,
        YouTubeProxyProvider provider,
        Long authId,
        long id,
        MessageBroadcaster messageBroadcaster,
        Room room,
        String ownerName
    ) {
        super(executorService, notificationService, provider, id, ownerName);
        this.messageId = messageId;
        this.executorService = executorService;
        this.httpClient = httpClient;
        this.proxyAuthService = proxyAuthService;
        this.authId = authId;
        this.messageBroadcaster = messageBroadcaster;
        this.room = room;
        this.ownerName = ownerName;
    }

    @Override
    protected void connect() {
        running.set(true);
        executorService.execute(this::doWork);
        started();
    }

    @Override
    protected void disconnect() {
        lastPageToken = null;
        liveChatId = null;
        running.set(false);
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        //todo
    }

    @Override
    public void onMessage(Message message) {

    }

    @Override
    public boolean outboundEnabled() {
        return false;
    }

    @Override
    public boolean moderationEnabled() {
        return true;
    }

    private void doWork() {
        if (running.get()) {
            try {
                if (liveChatId == null) {
                    liveChatId = getLiveChatId();
                    logger.debug("got chat id {}", liveChatId);
                }
                if (liveChatId != null) {
                    List<YouTubeMessage> messages = getMessages();
                    logger.trace("received messages", messages);
                    messages
                        .stream()
                        .filter(e -> e.getPublishedAt() > lastRead)
                        .map(e -> Message.extMessage(
                            room.getName(),
                            e.getName(),
                            LocalRole.USER,
                            GlobalRole.USER,
                            Colors.generateColor(e.getName()),
                            messageId.getAndIncrement(),
                            e.getPublishedAt(),
                            e.getMessage(),
                            "youtube",
                            ownerName
                        ))
                        .forEach(e -> messageBroadcaster.submitMessage(e, room.FILTER));
                    if (messages.size() > 0) {
                        lastRead = messages.get(messages.size() - 1).getPublishedAt();
                    }
                    executorService.schedule(this::doWork, pollInterval, TimeUnit.MILLISECONDS);
                } else {
                    executorService.schedule(this::doWork, 30, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                logger.warn("exception", e);
                fail(e.getMessage(), true);
            }
        }
    }

    private String getLiveChatId() throws IOException {
        String token = proxyAuthService.getToken(authId);
        HttpGet request = new HttpGet("https://www.googleapis.com/youtube/v3/liveBroadcasts" +
            "?part=snippet&broadcastStatus=active&broadcastType=all");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode root = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        long totalResults = root.get("pageInfo").get("totalResults").asLong();
        if (totalResults > 0) {
            JsonNode firstItem = root.get("items").get(0);
            JsonNode snippet = firstItem.get("snippet");
            return snippet.get("liveChatId").asText();
        }
        return null;
    }

    private List<YouTubeMessage> getMessages() throws IOException {
        String token = proxyAuthService.getToken(authId);
        String url = "https://www.googleapis.com/youtube/v3/liveChat/messages" +
            "?liveChatId=" + UrlEscapers.urlPathSegmentEscaper().escape(liveChatId) +
            "&part=snippet,authorDetails";
        if (lastPageToken != null) {
            url += "&pageToken=" + UrlEscapers.urlPathSegmentEscaper().escape(lastPageToken);
        }
        HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode root = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        List<YouTubeMessage> messages = new ArrayList<>();
        for (JsonNode item : root.get("items")) {
            JsonNode snippet = item.get("snippet");
            JsonNode authorDetails = item.get("authorDetails");
            messages.add(new YouTubeMessage(
                authorDetails.get("displayName").asText(),
                snippet.get("textMessageDetails").get("messageText").asText(),
                Instant.parse(snippet.get("publishedAt").asText()).toEpochMilli()
            ));
        }
        JsonNode offlineAt = root.get("offlineAt");
        if (offlineAt != null && !offlineAt.isNull()) {
            lastPageToken = null;
            liveChatId = null;
        } else {
            lastPageToken = root.get("nextPageToken").asText();
            pollInterval = root.get("pollingIntervalMillis").asInt();
        }
        return messages;
    }
}
