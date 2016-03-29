package lexek.wschat.proxy.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateHandler;
import lexek.wschat.chat.model.Message;
import lexek.wschat.proxy.AbstractProxy;
import lexek.wschat.proxy.ModerationOperation;
import lexek.wschat.proxy.ProxyProvider;
import lexek.wschat.proxy.ProxyState;
import lexek.wschat.proxy.twitter.entity.*;
import lexek.wschat.services.NotificationService;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TwitterStreamingClient extends AbstractProxy {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss Z yyyy");
    private String lastTweetId = null;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TwitterApiClient profileSource;
    private final Bootstrap bootstrap;
    private final TwitterCredentials credentials;
    private final Set<TwitterMessageConsumer> consumers = new CopyOnWriteArraySet<>();
    private volatile Channel channel;
    private volatile boolean changed = false;

    protected TwitterStreamingClient(
        NotificationService notificationService,
        EventLoopGroup eventLoopGroup,
        ProxyProvider provider,
        TwitterApiClient profileSource,
        TwitterCredentials credentials
    ) {
        super(eventLoopGroup, notificationService, provider, -1, "*internal*");
        this.profileSource = profileSource;
        try {
            this.bootstrap = createBootstrap(eventLoopGroup, new Handler());
        } catch (SSLException e) {
            //should not happen
            throw new RuntimeException(e);
        }
        this.credentials = credentials;
        eventLoopGroup.scheduleAtFixedRate((Runnable) () -> {
            if (state() == ProxyState.RUNNING) {
                logger.debug("running update task");
                if (changed) {
                    changed = false;
                    stop();
                    start();
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private static Bootstrap createBootstrap(EventLoopGroup eventLoopGroup, Handler handler) throws SSLException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        SslContext sslContext = SslContextBuilder.forClient().build();
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel c) throws Exception {
                ChannelPipeline pipeline = c.pipeline();
                pipeline.addLast(sslContext.newHandler(c.alloc()));
                pipeline.addLast(new HttpClientCodec(4096, 8192, 8192));
                pipeline.addLast(new IdleStateHandler(90, 0, 0));
                pipeline.addLast(new MessageToMessageDecoder<HttpContent>() {
                    @Override
                    protected void decode(ChannelHandlerContext channelHandlerContext, HttpContent httpContent, List<Object> out) throws Exception {
                        out.add(httpContent.content().retain());
                    }
                });
                pipeline.addLast(new DelimiterBasedFrameDecoder(102400, Unpooled.copiedBuffer("\r\n", StandardCharsets.UTF_8)));
                pipeline.addLast(handler);
            }
        });
        return bootstrap;
    }

    @Override
    protected void connect() {
        if (consumers.size() > 0) {
            ChannelFuture channelFuture = bootstrap.connect("stream.twitter.com", 443);
            channel = channelFuture.channel();
            channelFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    fail("failed to connect");
                }
            });
        } else {
            channel = null;
            started();
        }
    }

    @Override
    protected void disconnect() {
        if (channel != null) {
            channel.close();
        }
    }

    @Override
    public void moderate(ModerationOperation type, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMessage(Message message) {
        //do nothing
    }

    @Override
    public boolean outboundEnabled() {
        return false;
    }

    @Override
    public boolean moderationEnabled() {
        return false;
    }

    public synchronized void registerConsumer(TwitterMessageConsumer consumer) {
        logger.debug("registered consumer {} ({})", consumer.getEntityName(), consumer.getConsumerType());
        consumers.add(consumer);
        if (consumers.size() == 1) {
            start();
        } else {
            changed = true;
        }
    }

    public synchronized void deregisterConsumer(TwitterMessageConsumer consumer) {
        logger.debug("deregistered consumer {} ({})", consumer.getEntityName(), consumer.getConsumerType());
        consumers.remove(consumer);
        if (consumers.size() == 0) {
            stop();
        } else {
            changed = true;
        }
    }

    @ChannelHandler.Sharable
    private class Handler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            changed = false;
            List<String> tracks = new ArrayList<>();
            List<Long> follows = new ArrayList<>();
            for (TwitterMessageConsumer consumer : consumers) {
                switch (consumer.getConsumerType()) {
                    case TWEETS_HASHTAG:
                        tracks.add("#" + consumer.getEntityName());
                        break;
                    case TWEETS_LINK:
                        String url = consumer.getEntityName();
                        if (url.contains("/")) {
                            tracks.add(url);
                        } else {
                            tracks.add(url.replace('.', ' '));
                        }
                        break;
                    case TWEETS_PHRASE:
                        tracks.add(consumer.getEntityName());
                        break;
                    case TWEETS_ACCOUNT:
                        follows.add(profileSource.getTwitterId(consumer.getEntityName()));
                        break;
                    case TWEETS_SYMBOL:
                        tracks.add("$" + consumer.getEntityName());
                        break;
                }
            }

            String url = "https://stream.twitter.com/1.1/statuses/filter.json";
            Map<String, String> queryParameters = ImmutableMap.of(
                "track", tracks.stream().collect(Collectors.joining(",")),
                "follow", follows.stream().map(Object::toString).collect(Collectors.joining(","))
            );
            logger.debug("Starting with parameters {}", queryParameters);
            QueryStringEncoder queryStringEncoder = new QueryStringEncoder(url);
            queryParameters.forEach(queryStringEncoder::addParam);
            String wholeUrl = queryStringEncoder.toUri().toString();
            HttpMethod method = HttpMethod.POST;
            FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                wholeUrl
            );
            String header = OAuthUtil.generateAuthorizationHeader(
                credentials.getConsumerKey(),
                credentials.getConsumerSecret(),
                credentials.getAccessToken(),
                credentials.getAccessTokenSecret(),
                url,
                method,
                queryParameters
            );
            request.headers().add("Authorization", header);
            ctx.writeAndFlush(request);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!ctx.channel().isActive()) {
                return;
            }
            if (msg instanceof HttpResponse) {
                HttpResponse response = ((HttpResponse) msg);
                if (response.getStatus().code() == 200) {
                    started();
                } else {
                    fail(response.getStatus().reasonPhrase());
                }
            }
            if (msg instanceof ByteBuf) {
                String message = (((ByteBuf) msg).toString(StandardCharsets.UTF_8));
                if (!message.isEmpty()) {
                    logger.trace("{}: {}", ctx.channel().localAddress(), message);
                    JsonNode rootNode = objectMapper.readTree(message);
                    if (rootNode.hasNonNull("id")) {
                        handleTweet(rootNode);
                    } else {
                        if (rootNode.hasNonNull("limit")) {
                            logger.warn("limit {}", message);
                        } else if (rootNode.hasNonNull("disconnect")) {
                            logger.warn("disconnect notice {}", message);
                        } else if (rootNode.hasNonNull("warning")) {
                            logger.warn("warning {}", message);
                        } else {
                            logger.warn("unsupported message {}", message);
                        }
                    }
                }
            }
        }

        private void handleTweet(JsonNode rootNode) {
            Set<String> hashtags = new HashSet<>();
            Set<String> links = new HashSet<>();
            Set<String> symbols = new HashSet<>();
            for (JsonNode node : rootNode.get("entities").get("hashtags")) {
                hashtags.add(node.get("text").asText().toLowerCase());
            }
            for (JsonNode node : rootNode.get("entities").get("urls")) {
                links.add(node.get("expanded_url").asText());
            }
            for (JsonNode node : rootNode.get("entities").get("symbols")) {
                symbols.add(node.get("text").asText().toLowerCase());
            }
            Tweet tweet = processTweet(rootNode);
            if (tweet.getId().equals(lastTweetId)) {
                return;
            }
            lastTweetId = tweet.getId();
            String from = tweet.getFrom().toLowerCase();
            boolean simpleRetweet = tweet.getRetweetedStatus() != null && tweet.getQuotedStatus() == null;
            boolean reply = tweet.getReplyToStatus() != null;
            boolean replyToSelf = reply && tweet.getReplyToStatus().getFrom().equalsIgnoreCase(from);
            boolean simpleTweet = !simpleRetweet && !reply;
            Set<String> sentTo = new HashSet<>();
            for (TwitterMessageConsumer consumer : consumers) {
                if (sentTo.contains(consumer.getDestination())) {
                    continue;
                }
                switch (consumer.getConsumerType()) {
                    case TWEETS_HASHTAG:
                        if (simpleTweet && hashtags.contains(consumer.getEntityName())) {
                            consumer.onTweet(tweet);
                            sentTo.add(consumer.getDestination());
                        }
                        break;
                    case TWEETS_LINK:
                        if (simpleTweet && links.stream().anyMatch(s -> s.contains(consumer.getEntityName()))) {
                            consumer.onTweet(tweet);
                            sentTo.add(consumer.getDestination());
                        }
                        break;
                    case TWEETS_PHRASE:
                        if (simpleTweet && tweet.getText().toLowerCase().contains(consumer.getEntityName())) {
                            consumer.onTweet(tweet);
                            sentTo.add(consumer.getDestination());
                        }
                        break;
                    case TWEETS_ACCOUNT:
                        if ((!reply || replyToSelf) && from.equals(consumer.getEntityName())) {
                            consumer.onTweet(tweet);
                            sentTo.add(consumer.getDestination());
                        }
                        break;
                    case TWEETS_SYMBOL:
                        if (simpleTweet && symbols.contains(consumer.getEntityName())) {
                            consumer.onTweet(tweet);
                            sentTo.add(consumer.getDestination());
                        }
                }
            }
        }

        private Tweet processTweet(JsonNode tweetNode) {
            if (tweetNode == null || tweetNode.isNull()) {
                return null;
            }
            Tweet replyToStatus = null;
            if (tweetNode.hasNonNull("in_reply_to_status_id_str")) {
                String name = tweetNode.get("in_reply_to_screen_name").asText();
                String id = tweetNode.get("in_reply_to_status_id").asText();
                replyToStatus = new Tweet(id, name, null);
            }
            JsonNode retweetedStatus = tweetNode.get("retweeted_status");
            JsonNode userNode = tweetNode.get("user");
            String user = userNode.get("screen_name").asText();
            String fullName = userNode.get("name").asText();
            String avatarUrl = userNode.get("profile_image_url").asText();
            String text = tweetNode.get("text").asText();
            //we should render only original tweet body if tweet is retweet
            if (retweetedStatus == null || retweetedStatus.isNull()) {
                text = renderTweetText(tweetNode);
            }
            String id = tweetNode.get("id_str").asText();
            return new Tweet(
                id,
                user,
                fullName,
                avatarUrl,
                text,
                Instant.from(DATE_FORMATTER.parse(tweetNode.get("created_at").asText())).toEpochMilli(),
                processTweet(retweetedStatus),
                processTweet(tweetNode.get("quoted_status")),
                replyToStatus
            );
        }

        private String renderTweetText(JsonNode tweet) {
            TreeSet<TweetEntity> entities = new TreeSet<>();
            String originalText = tweet.get("text").asText();
            JsonNode entitiesNode = tweet.get("entities");
            if (entitiesNode != null && !entitiesNode.isNull()) {
                JsonNode mediaEntities = entitiesNode.get("media");
                JsonNode urlEntities = entitiesNode.get("urls");
                JsonNode mentionEntities = entitiesNode.get("user_mentions");
                JsonNode hashTagEntities = entitiesNode.get("hashtags");
                JsonNode symbolEntities = entitiesNode.get("symbols");
                if (mediaEntities != null) {
                    for (JsonNode entity : mediaEntities) {
                        JsonNode indices = entity.get("indices");
                        entities.add(new MediaEntity(
                            indices.get(0).asInt(),
                            indices.get(1).asInt(),
                            entity.get("url").asText(),
                            entity.get("display_url").asText()
                        ));
                    }
                }
                if (urlEntities != null) {
                    for (JsonNode entity : urlEntities) {
                        JsonNode indices = entity.get("indices");
                        entities.add(new UrlEntity(
                            indices.get(0).asInt(),
                            indices.get(1).asInt(),
                            entity.get("url").asText(),
                            entity.get("display_url").asText()
                        ));
                    }
                }
                if (mentionEntities != null) {
                    for (JsonNode entity : mentionEntities) {
                        JsonNode indices = entity.get("indices");
                        entities.add(new UserMentionEntity(
                            indices.get(0).asInt(),
                            indices.get(1).asInt(),
                            entity.get("screen_name").asText()
                        ));
                    }
                }
                if (hashTagEntities != null) {
                    for (JsonNode entity : hashTagEntities) {
                        JsonNode indices = entity.get("indices");
                        entities.add(new HashTagEntity(
                            indices.get(0).asInt(),
                            indices.get(1).asInt(),
                            entity.get("text").asText()
                        ));
                    }
                }
                if (symbolEntities != null) {
                    for (JsonNode entity : symbolEntities) {
                        JsonNode indices = entity.get("indices");
                        entities.add(new SymbolEntity(
                            indices.get(0).asInt(),
                            indices.get(1).asInt(),
                            entity.get("text").asText()
                        ));
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                int pos = 0;
                for (TweetEntity tweetEntity : entities) {
                    stringBuilder.append(originalText, pos, tweetEntity.getStart());
                    tweetEntity.render(stringBuilder);
                    pos = tweetEntity.getEnd();
                }
                stringBuilder.append(originalText, pos, originalText.length());
                return stringBuilder.toString();
            } else {
                return originalText;
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (state() == ProxyState.RUNNING) {
                minorFail("disconnected");
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.warn("exception", cause);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt == IdleState.READER_IDLE) {
                minorFail("timeout");
            }
        }
    }
}
