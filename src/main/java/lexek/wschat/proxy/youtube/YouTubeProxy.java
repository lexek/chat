package lexek.wschat.proxy.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import com.google.common.net.UrlEscapers;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.proxy.AbstractProxy;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.proxy.ProxyDescriptor;
import lexek.wschat.proxy.ProxyTokenException;
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
    private long lastRead = System.currentTimeMillis();
    private int pollInterval = 5000;
    private String lastPageToken = null;
    private String liveChatId = null;
    private String channelId;
    private String ownerName;

    //todo: custom response handler
    public YouTubeProxy(
        ProxyDescriptor descriptor,
        AtomicLong messageId, NotificationService notificationService,
        ScheduledExecutorService executorService,
        HttpClient httpClient,
        ProxyAuthService proxyAuthService,
        MessageBroadcaster messageBroadcaster
    ) {
        super(executorService, notificationService, descriptor);
        this.messageId = messageId;
        this.executorService = executorService;
        this.httpClient = httpClient;
        this.proxyAuthService = proxyAuthService;
        this.messageBroadcaster = messageBroadcaster;
    }

    @Override
    protected void init() {
        ownerName = proxyAuthService.getProfile(descriptor.authId()).getName();
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
    public String remoteRoom() {
        return ownerName;
    }

    private void doWork() {
        if (running.get()) {
            try {
                if (liveChatId == null) {
                    getLiveChatId();
                    logger.debug("got chat id {}", liveChatId);
                }
                if (liveChatId != null) {
                    List<YouTubeMessage> messages = getMessages();
                    logger.debug("received {} messages", messages.size());
                    logger.trace("received messages", messages);
                    Room room = descriptor.getRoom();
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
                            ImmutableList.of(MessageNode.textNode(e.getMessage())),
                            "youtube",
                            channelId,
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

    private void getLiveChatId() throws IOException {
        String token = proxyAuthService.getToken(descriptor.authId());
        HttpGet request = new HttpGet("https://www.googleapis.com/youtube/v3/liveBroadcasts" +
            "?part=snippet&broadcastStatus=active&broadcastType=all");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode root = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        long totalResults = root.get("pageInfo").get("totalResults").asLong();
        if (totalResults > 0) {
            JsonNode firstItem = root.get("items").get(0);
            JsonNode snippet = firstItem.get("snippet");
            this.channelId = snippet.get("channelId").asText();
            this.liveChatId = snippet.get("liveChatId").asText();
        }
    }

    private List<YouTubeMessage> getMessages() throws IOException {
        String token = proxyAuthService.getToken(descriptor.authId());
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
            String type = snippet.get("type").asText();
            //todo: handle bans
            if (type.equals("textMessageEvent")) {
                messages.add(new YouTubeMessage(
                    authorDetails.get("displayName").asText(),
                    snippet.get("textMessageDetails").get("messageText").asText(),
                    Instant.parse(snippet.get("publishedAt").asText()).toEpochMilli()
                ));
            }
        }
        JsonNode offlineAt = root.get("offlineAt");
        if (offlineAt != null && !offlineAt.isNull()) {
            logger.debug("broadcast went offline");
            lastPageToken = null;
            liveChatId = null;
        } else {
            lastPageToken = root.get("nextPageToken").asText();
            pollInterval = root.get("pollingIntervalMillis").asInt();
        }
        return messages;
    }
}
