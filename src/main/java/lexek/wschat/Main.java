package lexek.wschat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
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
import lexek.wschat.chat.handlers.*;
import lexek.wschat.db.dao.*;
import lexek.wschat.frontend.http.*;
import lexek.wschat.frontend.http.admin.AdminPageHandler;
import lexek.wschat.frontend.http.rest.EmailResource;
import lexek.wschat.frontend.http.rest.admin.*;
import lexek.wschat.frontend.irc.*;
import lexek.wschat.frontend.ws.WebSocketChatHandler;
import lexek.wschat.frontend.ws.WebSocketChatServer;
import lexek.wschat.frontend.ws.WebSocketConnectionGroup;
import lexek.wschat.frontend.ws.WebSocketProtocol;
import lexek.wschat.proxy.ChatProxyFactory;
import lexek.wschat.proxy.ProxyConfiguration;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.AuthenticationService;
import lexek.wschat.security.CaptchaService;
import lexek.wschat.security.ReCaptcha;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.SecurityFeature;
import lexek.wschat.security.jersey.UserParamValueFactoryProvider;
import lexek.wschat.security.social.TwitchTvSocialAuthService;
import lexek.wschat.services.*;
import lexek.wschat.services.poll.PollService;
import lexek.wschat.services.poll.PollState;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.mail.internet.InternetAddress;
import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

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

        SslContext sslContext = SslContextBuilder
            .forServer(new File("./cert.pem"), new File("./key.pk8"))
            .build();

        Gson gson = new Gson();
        Configuration settings = gson.fromJson(new String(Files.readAllBytes(Paths.get("./settings.json"))), Configuration.class);

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

        ConnectionManager connectionManager = new ConnectionManager(metricRegistry);
        MessageReactor messageReactor = new DefaultMessageReactor(metricRegistry);
        UserService userService = new UserService(connectionManager, userDao, journalService);
        ChatterService chatterService = new ChatterService(chatterDao, journalService);
        AuthenticationManager authenticationManager = new AuthenticationManager(ircHost, emailService, connectionManager, userAuthDao);

        ThreadFactory scheduledThreadFactory = new ThreadFactoryBuilder().setNameFormat("ANNOUNCEMENT_SCHEDULER_%d").build();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(scheduledThreadFactory);
        AtomicLong messageId = new AtomicLong(0);
        HistoryService historyService = new HistoryService(20, historyDao);
        MessageBroadcaster messageBroadcaster = new MessageBroadcaster(historyService, connectionManager);
        RoomManager roomManager = new RoomManager(userService, messageBroadcaster, roomDao, chatterService, journalService);
        AnnouncementService announcementService = new AnnouncementService(new AnnouncementDao(dataSource), journalService, roomManager, messageBroadcaster, scheduledExecutorService);
        PollService pollService = new PollService(new PollDao(dataSource), messageBroadcaster, roomManager, journalService);
        AuthenticationService authenticationService = new AuthenticationService(authenticationManager, userService, captchaService);
        NotificationService notificationService = new NotificationService(connectionManager, userService, messageBroadcaster, emailService, pendingNotificationDao);
        TicketService ticketService = new TicketService(userService, notificationService, new TicketDao(dataSource));
        EmoticonService emoticonService = new EmoticonService(emoticonDao, journalService);

        RoomJoinNotificationService roomJoinNotificationService = new RoomJoinNotificationService();
        roomJoinNotificationService.registerListener((connection, chatter, room) -> {
            PollState activePoll = pollService.getActivePoll(room);
            if (activePoll != null) {
                connection.send(Message.pollMessage(MessageType.POLL, room.getName(), activePoll));
                if (activePoll.getVoted().contains(connection.getUser().getId())) {
                    connection.send(Message.pollVotedMessage(room.getName()));
                }
            }
        });
        roomJoinNotificationService.registerListener((connection, chatter, room) ->
            announcementService.sendAnnouncements(connection, room));
        roomJoinNotificationService.registerListener((connection, chatter, room) -> {
            if (connection.isNeedNames()) {
                ImmutableList.Builder<Chatter> users = ImmutableList.builder();
                room.getOnlineChatters().stream().filter(c -> c.hasRole(LocalRole.USER)).forEach(users::add);
                connection.send(Message.namesMessage(room.getName(), users.build()));
            }
        });
        roomJoinNotificationService.registerListener(((connection, chatter, room) ->
            connection.send(Message.historyMessage(room.getHistory()))));
        roomJoinNotificationService.registerListener(notificationService);
        Set<String> bannedIps = new CopyOnWriteArraySet<>();
        messageReactor.registerHandler(new BanHandler(messageBroadcaster, roomManager));
        messageReactor.registerHandler(new ClearUserHandler(messageBroadcaster, roomManager));
        messageReactor.registerHandler(new ColorHandler(userDao));
        messageReactor.registerHandler(new JoinHandler(roomJoinNotificationService, roomManager, messageBroadcaster));
        messageReactor.registerHandler(new RestrictionFilter(captchaService,
            new MsgHandler(messageId, messageBroadcaster, roomManager), bannedIps));
        messageReactor.registerHandler(new RestrictionFilter(captchaService,
            new MeHandler(messageBroadcaster, messageId, roomManager), bannedIps));
        messageReactor.registerHandler(new PartHandler(messageBroadcaster, roomManager));
        messageReactor.registerHandler(new SetRoleHandler(roomManager));
        messageReactor.registerHandler(new TimeOutHandler(messageBroadcaster, roomManager));
        messageReactor.registerHandler(new UnbanHandler(roomManager));
        messageReactor.registerHandler(new NameHandler(userService));
        messageReactor.registerHandler(new LikeHandler(messageBroadcaster, roomManager));
        messageReactor.registerHandler(new ClearHandler(messageBroadcaster, roomManager));
        messageReactor.registerHandler(new VoteHandler(roomManager, pollService));

        serviceManager.registerService(announcementService);
        serviceManager.registerService(messageBroadcaster);
        serviceManager.registerService(messageReactor);
        serviceManager.registerService(authenticationService);
        serviceManager.registerService(roomJoinNotificationService);

        ChatProxyFactory chatProxyFactory = new ChatProxyFactory(connectionManager, messageId, authenticationManager,
            roomManager, messageBroadcaster, metricRegistry);
        for (ProxyConfiguration proxyConfiguration : settings.getProxy()) {
            serviceManager.registerService(chatProxyFactory.newInstance(proxyConfiguration));
        }

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

        ResourceConfig resourceConfig = new ResourceConfig() {
            {
                property(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE);
                //property(ServerProperties.TRACING, "ALL");
                register(ObjectMapperProvider.class);
                register(new Slf4jLoggingFilter());
                register(JerseyExceptionMapper.class);
                register(JacksonFeature.class);
                register(MultiPartFeature.class);
                register(SecurityFeature.class);
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
                    new AnnouncementResource(announcementService, roomManager),
                    new ChattersResource(chatterDao, roomManager),
                    new EmoticonsResource(dataDir, emoticonService),
                    new HistoryResource(historyService),
                    new IpBlockResource(bannedIps),
                    new JournalResource(journalDao),
                    new UsersResource(connectionManager, userService),
                    new PollResource(roomManager, pollService),
                    new RoomResource(roomManager),
                    new ServicesResource(serviceManager),
                    new TicketResource(ticketService),
                    new EmailResource(authenticationManager)
                );
            }
        };

        final RequestDispatcher httpRequestDispatcher = new RequestDispatcher(serverMessageHandler, viewResolvers, authenticationManager, resourceConfig);
        httpRequestDispatcher.add("/.*", new FileSystemStaticHandler(dataDir));
        httpRequestDispatcher.add("/.*", new ClassPathStaticHandler(ClassPathStaticHandler.class, "/static/"));
        httpRequestDispatcher.add("/chat.html", new ChatHomeHandler(settings.getHttp().isAllowLikes(), settings.getHttp().isSingleRoom()));
        httpRequestDispatcher.add("/resolve_steam", new SteamNameResolver());
        httpRequestDispatcher.add("/confirm_email", new ConfirmEmailHandler(authenticationManager, connectionManager));
        httpRequestDispatcher.add("/recaptcha/[0-9]+", new RecaptchaHandler(captchaService, reCaptcha));
        httpRequestDispatcher.add("/api/tickets", new UserTicketsHandler(authenticationManager, ticketService));
        httpRequestDispatcher.add("/admin/.*", new AdminPageHandler(authenticationManager));
        httpRequestDispatcher.add("/login", new LoginHandler(authenticationManager, reCaptcha));
        httpRequestDispatcher.add("/check_username", new UsernameCheckHandler(userService));
        httpRequestDispatcher.add("/twitch_auth", new TwitchAuthHandler(
            authenticationManager,
            new TwitchTvSocialAuthService(settings.getHttp().getTwitchClientId(),
                settings.getHttp().getTwitchSecret(),
                settings.getHttp().getTwitchUrl())));
        httpRequestDispatcher.add("/setup_profile", new SetupProfileHandler(authenticationManager, reCaptcha));
        httpRequestDispatcher.add("/register", new RegistrationHandler(authenticationManager, reCaptcha, bannedIps));
        httpRequestDispatcher.add("/password", new SetPasswordHandler(authenticationManager));
        httpRequestDispatcher.add("/token", new TokenHandler(authenticationManager));

        HttpServer httpServer = new HttpServer(sslContext, httpRequestDispatcher);

        serviceManager.registerService(webSocketChatServer);
        serviceManager.registerService(ircServer);
        serviceManager.registerService(httpServer);

        serviceManager.startAll();

        MetricReporter metricReporter = new MetricReporter(metricRegistry, "reporter", dataSource);
        metricReporter.start();
    }
}
