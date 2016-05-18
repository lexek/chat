package lexek.wschat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import freemarker.template.DefaultObjectWrapper;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lexek.httpserver.*;
import lexek.wschat.chat.*;
import lexek.wschat.chat.evt.ChatEventType;
import lexek.wschat.chat.evt.EventDispatcher;
import lexek.wschat.chat.handlers.*;
import lexek.wschat.chat.listeners.*;
import lexek.wschat.chat.processing.HandlerInvoker;
import lexek.wschat.db.dao.*;
import lexek.wschat.frontend.http.*;
import lexek.wschat.frontend.http.admin.AdminPageHandler;
import lexek.wschat.frontend.http.rest.*;
import lexek.wschat.frontend.http.rest.admin.*;
import lexek.wschat.frontend.irc.*;
import lexek.wschat.frontend.ws.WebSocketChatHandler;
import lexek.wschat.frontend.ws.WebSocketChatServer;
import lexek.wschat.frontend.ws.WebSocketConnectionGroup;
import lexek.wschat.frontend.ws.WebSocketProtocol;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.proxy.ProxyManager;
import lexek.wschat.proxy.SendProxyListOnEventListener;
import lexek.wschat.proxy.goodgame.GoodGameProxyProvider;
import lexek.wschat.proxy.sc2tv.Sc2tvProxyProvider;
import lexek.wschat.proxy.twitch.TwitchTvProxyProvider;
import lexek.wschat.proxy.twitter.TwitterApiClient;
import lexek.wschat.proxy.twitter.TwitterProxyProvider;
import lexek.wschat.proxy.youtube.YouTubeProxyProvider;
import lexek.wschat.security.*;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.SecurityFeature;
import lexek.wschat.security.jersey.UserParamValueFactoryProvider;
import lexek.wschat.security.social.*;
import lexek.wschat.services.*;
import lexek.wschat.services.poll.PollService;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.mail.internet.InternetAddress;
import javax.sql.DataSource;
import java.awt.*;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            context.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure("./logback.xml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SecureTokenGenerator secureTokenGenerator = new SecureTokenGenerator();

        int httpClientTimeout = 3000;
        PoolingHttpClientConnectionManager httpConnectionManager = new PoolingHttpClientConnectionManager();
        httpConnectionManager.setMaxTotal(50);
        httpConnectionManager.setDefaultMaxPerRoute(4);
        httpConnectionManager.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(httpClientTimeout).build());
        HttpClient httpClient = HttpClients
            .custom()
            .setDefaultRequestConfig(
                RequestConfig
                    .custom()
                    .setConnectionRequestTimeout(httpClientTimeout)
                    .setConnectTimeout(httpClientTimeout)
                    .setSocketTimeout(httpClientTimeout)
                    .build()
            )
            .setConnectionManager(httpConnectionManager)
            .build();

        SslContext sslContext = SslContextBuilder
            .forServer(new File("./cert.pem"), new File("./key.pk8"))
            .build();

        ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Configuration settings = objectMapper.readValue(Paths.get("./settings.json").toFile(), Configuration.class);
        CoreConfiguration coreSettings = settings.getCore();

        int wsPort = settings.getCore().getWsPort();
        String ircHost = settings.getCore().getHost();
        File dataDir = new File(settings.getCore().getDataDir());

        MetricRegistry metricRegistry = new MetricRegistry();
        MetricRegistry runtimeMetricRegistry = new MetricRegistry();
        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

        ServiceManager serviceManager = new ServiceManager(healthCheckRegistry, runtimeMetricRegistry);

        runtimeMetricRegistry.registerAll(new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
        runtimeMetricRegistry.registerAll(new GarbageCollectorMetricSet());
        runtimeMetricRegistry.registerAll(new JvmAttributeGaugeSet());
        runtimeMetricRegistry.registerAll(new MemoryUsageGaugeSet());
        runtimeMetricRegistry.register("socket.implementation", (Gauge<String>) () -> Epoll.isAvailable() ? "epoll" : "nio");
        runtimeMetricRegistry.register("ssl.implementation", (Gauge<String>) () -> OpenSsl.isAvailable() ? "openssl" : "java");
        runtimeMetricRegistry.register("ssl.cipherSuites", (Gauge<List<String>>) sslContext::cipherSuites);

        HikariConfig config = new HikariConfig();
        config.setPoolName("connection-pool");
        config.setJdbcUrl(settings.getDb().getUri());
        config.setUsername(settings.getDb().getUsername());
        config.setPassword(settings.getDb().getPassword());
        config.setMaximumPoolSize(settings.getCore().getPoolSize());
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));
        config.setMetricRegistry(runtimeMetricRegistry);
        config.setHealthCheckRegistry(healthCheckRegistry);
        DataSource dataSource = new HikariDataSource(config);

        EmailConfiguration emailSettings = settings.getEmail();
        EmailService emailService = new EmailService(
            emailSettings.getSmtpHost(),
            emailSettings.getSmtpPort(),
            new InternetAddress(emailSettings.getEmail(), emailSettings.getFromName()),
            emailSettings.getPassword(),
            emailSettings.getPrefix());

        UserDao userDao = new UserDao(dataSource);
        ChatterDao chatterDao = new ChatterDao(dataSource);
        EmoticonDao emoticonDao = new EmoticonDao(dataSource);
        RoomDao roomDao = new RoomDao(dataSource);
        JournalDao journalDao = new JournalDao(dataSource);
        JournalService journalService = new JournalService(journalDao);
        CaptchaService captchaService = new CaptchaService();
        HistoryDao historyDao = new HistoryDao(dataSource);
        PendingNotificationDao pendingNotificationDao = new PendingNotificationDao(dataSource);
        UserAuthDao userAuthDao = new UserAuthDao(dataSource);
        ProxyDao proxyDao = new ProxyDao(dataSource);
        IgnoreDao ignoreDao = new IgnoreDao(dataSource);

        ConnectionManager connectionManager = new ConnectionManager(metricRegistry);
        AuthenticationManager authenticationManager = new AuthenticationManager(ircHost, secureTokenGenerator, emailService, connectionManager, userAuthDao);
        UserService userService = new UserService(connectionManager, authenticationManager, userDao, journalService);
        EventDispatcher eventDispatcher = new EventDispatcher();

        ThreadFactory scheduledThreadFactory = new ThreadFactoryBuilder().setNameFormat("ANNOUNCEMENT_SCHEDULER_%d").build();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(scheduledThreadFactory);
        AtomicLong messageId = new AtomicLong(0);
        HistoryService historyService = new HistoryService(20, historyDao, userService);
        MessageConsumerServiceHandler messageConsumerServiceHandler = new MessageConsumerServiceHandler();
        MessageBroadcaster messageBroadcaster = new MessageBroadcaster();
        messageBroadcaster.registerConsumer(historyService);
        messageBroadcaster.registerConsumer(connectionManager);
        messageBroadcaster.registerConsumer(messageConsumerServiceHandler);
        ChatterService chatterService = new ChatterService(chatterDao, journalService, userService, messageBroadcaster);
        RoomManager roomManager = new RoomManager(userService, messageBroadcaster, roomDao, chatterService, journalService);
        RoomService roomService = new RoomService(roomManager);
        AnnouncementService announcementService = new AnnouncementService(new AnnouncementDao(dataSource), journalService, roomManager, messageBroadcaster, scheduledExecutorService);
        PollService pollService = new PollService(new PollDao(dataSource), messageBroadcaster, roomManager, journalService);
        AuthenticationService authenticationService = new AuthenticationService(authenticationManager, userService, captchaService, eventDispatcher);
        NotificationService notificationService = new NotificationService(connectionManager, userService, messageBroadcaster, emailService, pendingNotificationDao);
        TicketService ticketService = new TicketService(userService, notificationService, new TicketDao(dataSource));
        EmoticonService emoticonService = new EmoticonService(
            new Dimension(coreSettings.getMaxEmoticonWidth(), coreSettings.getMaxEmoticonHeight()),
            dataDir,
            emoticonDao,
            journalService,
            messageBroadcaster
        );
        SteamGameDao steamGameDao = new SteamGameDao(dataSource);
        SteamGameResolver steamGameResolver = new SteamGameResolver(steamGameDao, httpClient);

        IgnoreService ignoreService = new IgnoreService(ignoreDao);

        Set<String> bannedIps = new CopyOnWriteArraySet<>();

        ProxyAuthDao proxyAuthDao = new ProxyAuthDao(dataSource);
        SocialAuthCredentials twitch = settings.getProxy().get("twitch");
        SocialAuthCredentials google = settings.getProxy().get("google");
        SocialAuthCredentials goodGame = settings.getProxy().get("goodgame");
        ProxyAuthService proxyAuthService = new ProxyAuthService(
            ImmutableMap.of(
                "twitch", new TwitchTvSocialAuthProvider(
                    twitch.getClientId(),
                    twitch.getClientSecret(),
                    twitch.getRedirectUrl(),
                    httpClient,
                    secureTokenGenerator,
                    "twitch"
                ),
                "google", new GoogleSocialAuthProvider(
                    google.getClientId(),
                    google.getClientSecret(),
                    google.getRedirectUrl(),
                    ImmutableSet.of(
                        "https://www.googleapis.com/auth/youtube.readonly",
                        "profile",
                        "email"
                    ),
                    "google",
                    httpClient,
                    secureTokenGenerator
                ),
                "goodgame", new GoodGameSocialAuthProvider(
                    goodGame.getClientId(),
                    goodGame.getClientSecret(),
                    goodGame.getRedirectUrl(),
                    "goodgame",
                    httpClient,
                    secureTokenGenerator
                )
            ),
            proxyAuthDao
        );
        proxyAuthService.loadTokens();

        EventLoopGroup proxyEventLoopGroup;
        if (Epoll.isAvailable()) {
            proxyEventLoopGroup = new EpollEventLoopGroup(1);
        } else {
            proxyEventLoopGroup = new NioEventLoopGroup(1);
        }
        ProxyManager proxyManager = new ProxyManager(proxyDao, roomManager, journalService);
        proxyManager.registerProvider(new GoodGameProxyProvider(notificationService, proxyAuthService, proxyEventLoopGroup, messageBroadcaster, messageId, httpClient));
        proxyManager.registerProvider(new TwitchTvProxyProvider(messageId, messageBroadcaster, authenticationManager, proxyEventLoopGroup, proxyAuthService, notificationService));
        proxyManager.registerProvider(new Sc2tvProxyProvider(notificationService, proxyEventLoopGroup, messageBroadcaster, messageId));
        proxyManager.registerProvider(new YouTubeProxyProvider(messageId, messageBroadcaster, notificationService, scheduledExecutorService, proxyAuthService, httpClient));
        if (settings.getTwitter() != null) {
            TwitterApiClient twitterApiClient = new TwitterApiClient(httpClient, settings.getTwitter());
            twitterApiClient.loadNames(
                proxyDao
                    .getAll()
                    .stream()
                    .filter(chatProxy -> chatProxy.getProviderName().equals("twitter"))
                    .filter(chatProxy -> chatProxy.getRemoteRoom().startsWith("@"))
                    .map(chatProxy -> chatProxy.getRemoteRoom().substring(1))
                    .collect(Collectors.toList())
            );

            proxyManager.registerProvider(new TwitterProxyProvider(
                notificationService,
                messageBroadcaster, proxyEventLoopGroup,
                settings.getTwitter(),
                twitterApiClient
            ));
        }
        messageConsumerServiceHandler.register(proxyManager);
        eventDispatcher.registerListener(ChatEventType.CONNECT, new SendIgnoreListOnEventListener(ignoreService));
        eventDispatcher.registerListener(ChatEventType.CONNECT, new SendEmoticonsOnEventListener(emoticonService));
        eventDispatcher.registerListener(ChatEventType.JOIN, new SendProxyListOnEventListener(proxyManager));
        eventDispatcher.registerListener(ChatEventType.JOIN, new SendNamesOnEventListener());
        eventDispatcher.registerListener(ChatEventType.JOIN, new SendHistoryOnEventListener());
        eventDispatcher.registerListener(ChatEventType.JOIN, new SendTopicOnEventListener());
        eventDispatcher.registerListener(ChatEventType.JOIN, new SendAnnouncementsOnEventListener(announcementService));
        eventDispatcher.registerListener(ChatEventType.JOIN, new SendPollOnEventListener(pollService));
        eventDispatcher.registerListener(ChatEventType.JOIN, new SendNotificationsOnEventListener(notificationService));

        HandlerInvoker handlerInvoker = new HandlerInvoker(roomManager, chatterService, bannedIps, captchaService);
        MessageReactor messageReactor = new DefaultMessageReactor(handlerInvoker);
        handlerInvoker.register(new BanHandler(chatterService));
        handlerInvoker.register(new ClearUserHandler(messageBroadcaster));
        handlerInvoker.register(new ColorHandler(userDao));
        handlerInvoker.register(new JoinHandler(eventDispatcher, messageBroadcaster));
        handlerInvoker.register(new MsgHandler(messageId, messageBroadcaster));
        handlerInvoker.register(new MeHandler(messageBroadcaster, messageId));
        handlerInvoker.register(new PartHandler(messageBroadcaster));
        handlerInvoker.register(new SetRoleHandler(chatterService));
        handlerInvoker.register(new TimeOutHandler(chatterService));
        handlerInvoker.register(new UnbanHandler(chatterService));
        handlerInvoker.register(new NameHandler(userService));
        handlerInvoker.register(new LikeHandler(messageBroadcaster));
        handlerInvoker.register(new ClearRoomHandler(messageBroadcaster));
        handlerInvoker.register(new VoteHandler(pollService, messageBroadcaster));
        handlerInvoker.register(new ProxyModerationHandler(proxyManager));
        handlerInvoker.register(new IgnoreHandler(ignoreService));
        handlerInvoker.register(new UnignoreHandler(ignoreService));

        serviceManager.registerService(announcementService);
        serviceManager.registerService(messageBroadcaster);
        serviceManager.registerService(messageReactor);
        serviceManager.registerService(authenticationService);
        serviceManager.registerService(eventDispatcher);
        serviceManager.registerService(proxyManager);

        EventLoopGroup bossGroup;
        EventLoopGroup childGroup;
        ThreadFactory bossThreadFactory = new ThreadFactoryBuilder().setNameFormat("MAIN_BOSS_THREAD_%d").build();
        ThreadFactory childThreadFactory = new ThreadFactoryBuilder().setNameFormat("MAIN_CHILD_THREAD_%d").build();
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(1, bossThreadFactory);
            childGroup = new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors(), childThreadFactory);
        } else {
            bossGroup = new NioEventLoopGroup(1, bossThreadFactory);
            childGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), childThreadFactory);
        }

        WebSocketProtocol webSocketProtocol = new WebSocketProtocol();
        WebSocketConnectionGroup webSocketConnectionGroup = new WebSocketConnectionGroup(webSocketProtocol.getCodec());
        connectionManager.registerGroup(webSocketConnectionGroup);
        WebSocketChatHandler webSocketChatHandler = new WebSocketChatHandler(authenticationService, messageReactor,
            webSocketProtocol, webSocketConnectionGroup, roomManager);
        WebSocketChatServer webSocketChatServer = new WebSocketChatServer(wsPort, webSocketChatHandler, bossGroup,
            childGroup, sslContext);

        IrcProtocol ircProtocol = new IrcProtocol(new IrcCodec(ircHost));
        IrcConnectionGroup ircConnectionGroup = new IrcConnectionGroup(ircProtocol.getCodec());
        connectionManager.registerGroup(ircConnectionGroup);
        IrcServerHandler ircServerHandler = new IrcServerHandler(messageReactor, ircHost,
            authenticationService, ircConnectionGroup, roomManager, ircProtocol);
        IrcServer ircServer = new IrcServer(ircServerHandler, bossGroup, childGroup, sslContext);

        freemarker.template.Configuration freemarker = new freemarker.template.Configuration();
        freemarker.setClassForTemplateLoading(Main.class, "/templates");
        freemarker.setDefaultEncoding("UTF-8");
        freemarker.setObjectWrapper(new DefaultObjectWrapper());

        ReCaptcha reCaptcha = new ReCaptcha(settings.getHttp().getRecaptchaKey());
        ViewResolvers viewResolvers = new ViewResolvers(freemarker);
        ServerMessageHandler serverMessageHandler = new ServerMessageHandler();

        SocialAuthService socialAuthService = new SocialAuthService(authenticationManager, secureTokenGenerator);
        SocialAuthCredentials twitchAuth = settings.getSocialAuth().get("twitch");
        SocialAuthCredentials googleAuth = settings.getSocialAuth().get("google");
        SocialAuthCredentials goodGameAuth = settings.getSocialAuth().get("goodgame");
        SocialAuthCredentials twitterAuth = settings.getSocialAuth().get("twitter");
        socialAuthService.registerProvider(new TwitchTvSocialAuthProvider(
            twitchAuth.getClientId(),
            twitchAuth.getClientSecret(),
            twitchAuth.getRedirectUrl(),
            httpClient,
            secureTokenGenerator,
            "twitch"
        ));
        socialAuthService.registerProvider(new TwitterSocialAuthProvider(
            true,
            false,
            twitterAuth.getClientId(),
            twitterAuth.getClientSecret(),
            twitterAuth.getRedirectUrl(),
            "twitter",
            httpClient
        ));
        socialAuthService.registerProvider(new GoodGameSocialAuthProvider(
            goodGameAuth.getClientId(),
            goodGameAuth.getClientSecret(),
            goodGameAuth.getRedirectUrl(),
            "goodgame",
            httpClient,
            secureTokenGenerator
        ));
        socialAuthService.registerProvider(new GoogleSocialAuthProvider(
            googleAuth.getClientId(),
            googleAuth.getClientSecret(),
            googleAuth.getRedirectUrl(),
            ImmutableSet.of(
                "profile",
                "email"
            ),
            "google",
            httpClient,
            secureTokenGenerator
        ));

        ResourceConfig resourceConfig = new ResourceConfig() {
            {
                property(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE);
                //property(ServerProperties.TRACING, "ALL");
                register(ErrorBodyWriter.class);
                register(ObjectMapperProvider.class);
                register(new Slf4jLoggingFilter());
                register(JerseyExceptionMapper.class);
                register(JacksonFeature.class);
                register(MultiPartFeature.class);
                register(SecurityFeature.class);
                register(FreemarkerMvcFeature.class);
                property(FreemarkerMvcFeature.TEMPLATE_BASE_PATH, "/templates/");
                property(FreemarkerMvcFeature.ENCODING, "UTF-8");
                register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(UserParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class);
                        bind(UserParamValueFactoryProvider.InjectionResolver.class).to(
                            new TypeLiteral<InjectionResolver<Auth>>() {
                            }).in(Singleton.class);
                    }
                });
                registerInstances(
                    new StatisticsResource(new StatisticsDao(dataSource), runtimeMetricRegistry, healthCheckRegistry),
                    new AnnouncementResource(announcementService, roomService),
                    new ChattersResource(chatterDao, roomService, chatterService, userService),
                    new EmoticonsResource(emoticonService),
                    new HistoryResource(historyService),
                    new IpBlockResource(bannedIps),
                    new JournalResource(journalDao),
                    new UsersResource(connectionManager, userService),
                    new PollResource(roomService, pollService),
                    new RoomResource(roomService),
                    new TicketResource(ticketService),
                    new EmailResource(authenticationManager),
                    new ProfileResource(userService),
                    new SteamGameResource(steamGameResolver),
                    new ProxyResource(roomService, proxyManager, proxyAuthService),
                    new PasswordResource(authenticationManager),
                    new CheckUsernameResource(userService),
                    new ProxyAuthResource(proxyAuthService),
                    new AuthResource(socialAuthService, authenticationManager, reCaptcha)
                );
            }
        };

        final RequestDispatcher httpRequestDispatcher = new RequestDispatcher(serverMessageHandler, viewResolvers, authenticationManager, resourceConfig, runtimeMetricRegistry, ircHost);
        httpRequestDispatcher.add("/", new RedirectToAppHandler());
        httpRequestDispatcher.add("/.*", new FileSystemStaticHandler(dataDir));
        httpRequestDispatcher.add("/.*", new ClassPathStaticHandler(ClassPathStaticHandler.class, "/static/"));
        httpRequestDispatcher.add("/chat.html", new ChatHomeHandler(coreSettings.getTitle(), settings.getHttp().isAllowLikes(), settings.getHttp().isSingleRoom()));
        httpRequestDispatcher.add("/confirm_email", new ConfirmEmailHandler());
        httpRequestDispatcher.add("/recaptcha/[0-9]+", new RecaptchaHandler(captchaService, reCaptcha));
        httpRequestDispatcher.add("/api/tickets", new UserTicketsHandler(authenticationManager, ticketService));
        httpRequestDispatcher.add("/admin/.*", new AdminPageHandler(authenticationManager));
        httpRequestDispatcher.add("/login", new LoginHandler(authenticationManager, reCaptcha));
        httpRequestDispatcher.add("/register", new RegistrationHandler(authenticationManager, reCaptcha, bannedIps));
        httpRequestDispatcher.add("/token", new TokenHandler(authenticationManager));

        HttpServer httpServer = new HttpServer(sslContext, httpRequestDispatcher);

        serviceManager.registerService(webSocketChatServer);
        serviceManager.registerService(ircServer);
        serviceManager.registerService(httpServer);

        serviceManager.startAll();

        MetricReporter metricReporter = new MetricReporter(metricRegistry, "reporter", dataSource);
        metricReporter.start();

        String graphiteHost = settings.getCore().getGraphiteServer();
        if (graphiteHost != null) {
            GraphiteSender graphiteSender = new Graphite(graphiteHost, 2003);
            GraphiteReporter.forRegistry(runtimeMetricRegistry)
                .prefixedWith(settings.getCore().getGraphitePrefix())
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .build(graphiteSender)
                .start(1, TimeUnit.MINUTES);
        }
    }
}
