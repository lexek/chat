package lexek.wschat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import freemarker.template.DefaultObjectWrapper;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lexek.httpserver.*;
import lexek.wschat.chat.*;
import lexek.wschat.chat.handlers.*;
import lexek.wschat.db.dao.*;
import lexek.wschat.frontend.http.*;
import lexek.wschat.frontend.http.admin.*;
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
import lexek.wschat.security.social.TwitchTvSocialAuthService;
import lexek.wschat.services.*;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
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

        Gson gson = new Gson();
        Configuration settings = gson.fromJson(new String(Files.readAllBytes(Paths.get("./settings.json"))), Configuration.class);

        int wsPort = settings.getCore().getWsPort();
        String ircHost = settings.getCore().getHost();
        File dataDir = new File(settings.getCore().getDataDir());

        ServiceManager serviceManager = new ServiceManager();
        MetricRegistry metricRegistry = new MetricRegistry();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.getDb().getUri());
        config.setUsername(settings.getDb().getUsername());
        config.setPassword(settings.getDb().getPassword());
        config.setMaximumPoolSize(settings.getCore().getPoolSize());
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));
        DataSource dataSource = new HikariDataSource(config);

        UserDao userDao = new UserDao(dataSource);
        ChatterDao chatterDao = new ChatterDao(dataSource);
        EmoticonDao emoticonDao = new EmoticonDao(dataSource);
        RoomDao roomDao = new RoomDao(dataSource);
        JournalDao journalDao = new JournalDao(dataSource);
        JournalService journalService = new JournalService(journalDao);
        AuthenticationManager authenticationManager = new AuthenticationManager(ircHost, dataSource, settings.getEmail());
        CaptchaService captchaService = new CaptchaService();
        HistoryDao historyDao = new HistoryDao(dataSource);

        ConnectionManager connectionManager = new ConnectionManager(metricRegistry);
        MessageReactor messageReactor = new DefaultMessageReactor(metricRegistry);
        UserService userService = new UserService(connectionManager, userDao, journalService);

        ThreadFactory scheduledThreadFactory = new ThreadFactoryBuilder().setNameFormat("ANNOUNCEMENT_SCHEDULER_%d").build();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(scheduledThreadFactory);
        AtomicLong messageId = new AtomicLong(0);
        HistoryService historyService = new HistoryService(20, historyDao);
        MessageBroadcaster messageBroadcaster = new MessageBroadcaster(historyService, connectionManager);
        RoomManager roomManager = new RoomManager(userService, messageBroadcaster, roomDao, chatterDao, journalService);
        AnnouncementService announcementService = new AnnouncementService(new AnnouncementDao(dataSource), journalService, roomManager, messageBroadcaster, scheduledExecutorService);
        PollService pollService = new PollService(new PollDao(dataSource), messageBroadcaster, roomManager, journalService);
        AuthenticationService authenticationService = new AuthenticationService(authenticationManager, userService, captchaService);
        TicketService ticketService = new TicketService(new TicketDao(dataSource), messageBroadcaster);
        EmoticonService emoticonService = new EmoticonService(emoticonDao, journalService);

        Set<String> bannedIps = new CopyOnWriteArraySet<>();
        messageReactor.registerHandler(new BanHandler(messageBroadcaster, roomManager));
        messageReactor.registerHandler(new ClearUserHandler(messageBroadcaster, roomManager));
        messageReactor.registerHandler(new ColorHandler(userDao));
        messageReactor.registerHandler(new JoinHandler(roomManager, announcementService, messageBroadcaster, pollService));
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

        ChatProxyFactory chatProxyFactory = new ChatProxyFactory(connectionManager, messageId, authenticationManager,
                roomManager, messageBroadcaster, metricRegistry);
        for (ProxyConfiguration proxyConfiguration : settings.getProxy()) {
            serviceManager.registerService(chatProxyFactory.newInstance(proxyConfiguration));
        }

        SslContext sslContext = SslContextBuilder
                .forServer(new File("./cert.pem"), new File("./key.pk8"))
                .build();

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
        //uncomment to disable template caching
        //freemarker.setCacheStorage(new NullCacheStorage());

        ReCaptcha reCaptcha = new ReCaptcha(settings.getHttp().getRecaptchaKey());
        ViewResolvers viewResolvers = new ViewResolvers(freemarker);
        ServerMessageHandler serverMessageHandler = new ServerMessageHandler();

        final RequestDispatcher httpRequestDispatcher = new RequestDispatcher(serverMessageHandler, viewResolvers);
        httpRequestDispatcher.add("/.*", new FileSystemStaticHandler(dataDir));
        httpRequestDispatcher.add("/.*", new ClassPathStaticHandler(ClassPathStaticHandler.class, "/static/"));
        httpRequestDispatcher.add("/chat.html", new ChatHomeHandler(settings.getHttp().isAllowLikes(), settings.getHttp().isSingleRoom()));
        httpRequestDispatcher.add("/resolve_steam", new SteamNameResolver());
        httpRequestDispatcher.add("/confirm_email", new ConfirmEmailHandler(authenticationManager, connectionManager));
        httpRequestDispatcher.add("/recaptcha/[0-9]+", new RecaptchaHandler(captchaService, reCaptcha));
        httpRequestDispatcher.add("/api/chatters", new ChattersApiHandler(roomManager));
        httpRequestDispatcher.add("/api/tickets", new UserTicketsHandler(authenticationManager, ticketService));
        httpRequestDispatcher.add("/admin/api/metrics", new MetricHandler(dataSource, authenticationManager));
        httpRequestDispatcher.add("/admin/api/journal", new JournalHandler(authenticationManager, new JournalDao(dataSource)));
        httpRequestDispatcher.add("/admin/api/rooms", new RoomsApiHandler(authenticationManager, roomManager));
        httpRequestDispatcher.add("/admin/api/room", new RoomApiHandler(authenticationManager, roomManager, historyService, announcementService));
        httpRequestDispatcher.add("/admin/api/poll", new PollApiHandler(authenticationManager, roomManager, pollService));
        httpRequestDispatcher.add("/admin/api/polls", new PollsApiHandler(authenticationManager, roomManager, pollService));
        httpRequestDispatcher.add("/admin/api/history", new HistoryApiHandler(authenticationManager, historyService));
        httpRequestDispatcher.add("/admin/api/tickets", new TicketsHandler(authenticationManager, ticketService));
        httpRequestDispatcher.add("/admin/api/ticket_count", new TicketCountHandler(authenticationManager, ticketService));
        httpRequestDispatcher.add("/admin/api/emoticons", new EmoticonHandler(dataDir, emoticonService, authenticationManager));
        httpRequestDispatcher.add("/admin/api/user", new UserApiHandler(authenticationManager, userService));
        httpRequestDispatcher.add("/admin/api/users", new UsersHandler(authenticationManager, userService));
        httpRequestDispatcher.add("/admin/api/chatters", new ChattersAdminApiHandler(authenticationManager, chatterDao));
        httpRequestDispatcher.add("/admin/api/online", new OnlineHandler(authenticationManager, connectionManager));
        httpRequestDispatcher.add("/admin/api/blockedip", new IpBlockHandler(bannedIps, authenticationManager));
        httpRequestDispatcher.add("/admin/api/announcement", new AnnouncementApiHandler(authenticationManager, announcementService));
        httpRequestDispatcher.add("/admin/api/services", new ServiceApiHandler(authenticationManager, serviceManager));
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
