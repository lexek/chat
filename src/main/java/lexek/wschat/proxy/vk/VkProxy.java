package lexek.wschat.proxy.vk;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.LocalRole;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.msg.MessageNode;
import lexek.wschat.proxy.AbstractProxy;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.proxy.ProxyDescriptor;
import lexek.wschat.proxy.ProxyTokenException;
import lexek.wschat.services.NotificationService;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class VkProxy extends AbstractProxy {
    private final ScheduledExecutorService scheduler;
    private final ProxyAuthService proxyAuthService;
    private final HttpClient httpClient;
    private final MessageBroadcaster messageBroadcaster;
    private final AtomicLong messageId;
    private volatile ScheduledFuture workFuture = null;
    private volatile ScheduledFuture checkFuture = null;
    private volatile Long lastId = null;
    private volatile String videoId;
    private final long since = (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2)) / 1000;
    private String videoName;
    private final VkApiClient vkApiClient;

    public VkProxy(
        ProxyDescriptor descriptor,
        ScheduledExecutorService scheduler,
        NotificationService notificationService,
        ProxyAuthService proxyAuthService,
        HttpClient httpClient,
        MessageBroadcaster messageBroadcaster,
        AtomicLong messageId,
        VkApiClient vkApiClient
    ) {
        super(scheduler, notificationService, descriptor);
        this.scheduler = scheduler;
        this.proxyAuthService = proxyAuthService;
        this.httpClient = httpClient;
        this.messageBroadcaster = messageBroadcaster;
        this.messageId = messageId;
        this.vkApiClient = vkApiClient;
    }

    @Override
    protected void init() throws Exception {
    }

    @Override
    protected void connect() {
        this.workFuture = scheduler.schedule(this::doWork, 0, TimeUnit.SECONDS);
        started();
    }

    @Override
    protected void disconnect() {
        if (workFuture != null && !workFuture.isCancelled()) {
            workFuture.cancel(false);
        }
        if (checkFuture != null && !checkFuture.isCancelled()) {
            checkFuture.cancel(false);
        }
    }

    private void doWork() {
        try {
            if (videoId == null) {
                logger.debug("trying to get live video");
                JsonNode video = vkApiClient.findLiveVideo(descriptor.getRemoteRoom(), proxyAuthService.getToken(descriptor.getAuthId().get()));
                if (video != null) {
                    this.videoId = video.get("id").asText();
                    this.videoName = video.get("title").asText();
                    if (checkFuture != null && !checkFuture.isCancelled()) {
                        checkFuture.cancel(false);
                    }
                    this.checkFuture = scheduler.scheduleAtFixedRate(this::checkVideoStillLive, 1, 1, TimeUnit.MINUTES);
                } else {
                    logger.debug("no live video found");
                }
            }

            if (videoId == null) {
                logger.debug("scheduling live video search");
                this.workFuture = scheduler.schedule(this::doWork, 1, TimeUnit.MINUTES);
            } else {
                logger.debug("scheduling next work");
                getNewMessages();
                this.workFuture = scheduler.schedule(this::doWork, 20, TimeUnit.SECONDS);
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

    private void checkVideoStillLive() {
        try {
            logger.debug("checking that video is still live");
            if (videoId != null) {
                JsonNode video = vkApiClient.getVideoInfo(descriptor.getRemoteRoom() + "_" + videoId, proxyAuthService.getToken(descriptor.getAuthId().get()));
                if (video == null || !video.has("live") || video.get("live").intValue() != 1) {
                    logger.debug("video is not live");
                    videoId = null;
                    if (checkFuture != null && !checkFuture.isCancelled()) {
                        checkFuture.cancel(false);
                    }
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

    private void getNewMessages() throws Exception {
        URIBuilder uriBuilder = new URIBuilder("https://api.vk.com/method/video.getComments");
        uriBuilder.addParameter("access_token", proxyAuthService.getToken(descriptor.getAuthId().get()));
        uriBuilder.addParameter("owner_id", descriptor.getRemoteRoom());
        uriBuilder.addParameter("video_id", videoId);
        uriBuilder.addParameter("count", "100");
        uriBuilder.addParameter("extended", "1");
        uriBuilder.addParameter("v", VkProxyProvider.API_VERSION);
        if (lastId != null) {
            uriBuilder.addParameter("start_comment_id", lastId.toString());
        }
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        JsonNode rootNode = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
        JsonNode responseNode = rootNode.get("response");
        for (JsonNode node : responseNode.get("items")) {
            long id = Long.parseLong(node.get("id").asText());
            boolean pass = true;
            if (lastId == null || id > lastId) {
                pass = false;
                long createdAt = Long.parseLong(node.get("date").asText());
                if (createdAt < since) {
                    pass = true;
                }
                lastId = id;
            }
            if (!pass) {
                logger.trace("got message: {}", node);
                //todo: name & chat icons
                //todo: emoji & link processor
                //todo: attachments https://vk.com/dev/objects/attachments_w
                messageBroadcaster.submitMessage(
                    Message.extMessage(
                        descriptor.getRoom().getName(),
                        "name",
                        LocalRole.USER,
                        GlobalRole.USER,
                        "green",
                        messageId.incrementAndGet(),
                        System.currentTimeMillis(),
                        ImmutableList.of(MessageNode.textNode(node.get("text").textValue())),
                        "vk",
                        descriptor.getRemoteRoom(),
                        videoName
                    ),
                    descriptor.getRoom().FILTER
                );
            }
        }
    }
}
