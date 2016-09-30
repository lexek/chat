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
import lexek.wschat.frontend.http.*;
import lexek.wschat.frontend.http.admin.AdminPageHandler;
import lexek.wschat.frontend.http.rest.RedirectToAppHandler;
import lexek.wschat.proxy.ProxyManager;
import lexek.wschat.proxy.twitch.TwitchCredentialsService;
import lexek.wschat.proxy.twitter.TwitterCredentials;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.security.CaptchaService;
import lexek.wschat.security.ReCaptcha;
import lexek.wschat.security.jersey.Auth;
import lexek.wschat.security.jersey.SecurityFeature;
import lexek.wschat.security.jersey.UserParamValueFactoryProvider;
import lexek.wschat.security.social.CredentialsHolder;
import lexek.wschat.services.MessageConsumerServiceHandler;
import lexek.wschat.services.TicketService;
import lexek.wschat.services.managed.ServiceManager;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.extras.ExtrasUtilities;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.awt.*;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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

        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.enableLookupExceptions(serviceLocator);

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
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .build()
            )
            .setConnectionManager(httpConnectionManager)
            .build();
        ServiceLocatorUtilities.addOneConstant(serviceLocator, httpClient, "httpClient", HttpClient.class);

        SslContext sslContext = SslContextBuilder
            .forServer(new File("./cert.pem"), new File("./key.pk8"))
            .build();

        ServiceLocatorUtilities.addOneConstant(serviceLocator, sslContext, "sslContext", SslContext.class);

        ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Configuration settings = objectMapper.readValue(Paths.get("./settings.json").toFile(), Configuration.class);
        CoreConfiguration coreSettings = settings.getCore();
        String hostName = settings.getCore().getHost();
        File dataDir = new File(settings.getCore().getDataDir());
        EmailConfiguration emailSettings = settings.getEmail();

        //todo: config rework https://github.com/lexek/chat/issues/233
        ServiceLocatorUtilities.addOneConstant(serviceLocator, hostName, "core.hostname", String.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, dataDir, "core.dataDirectory", File.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, "https://" + hostName + ":1337", "http.baseUrl", String.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, settings.getCore().getWsPort(), "websocket.port", Integer.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, settings.getHttp().getRecaptchaKey(), "recaptcha.secret", String.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, emailSettings.getSmtpHost(), "email.server", String.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, emailSettings.getSmtpPort(), "email.port", Integer.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, emailSettings.getEmail(), "email.from", String.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, emailSettings.getFromName(), "email.fromName", String.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, emailSettings.getPassword(), "email.password", String.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, emailSettings.getPrefix(), "email.prefix", String.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, 20, "history.pageLength", Integer.class);
        ServiceLocatorUtilities.addOneConstant(
            serviceLocator,
            new Dimension(coreSettings.getMaxEmoticonWidth(), coreSettings.getMaxEmoticonHeight()),
            "emoticon.maxDimensions",
            Dimension.class
        );
        ServiceLocatorUtilities.addOneConstant(
            serviceLocator,
            new CredentialsHolder(settings.getSocialAuth()),
            "social.credentials",
            CredentialsHolder.class
        );
        ServiceLocatorUtilities.addOneConstant(
            serviceLocator,
            new CredentialsHolder(settings.getProxy()),
            "proxy.credentials",
            CredentialsHolder.class
        );
        ServiceLocatorUtilities.addOneConstant(
            serviceLocator,
            settings.getTwitter(),
            "twitter.credentials",
            TwitterCredentials.class
        );

        MetricRegistry metricRegistry = new MetricRegistry();
        MetricRegistry runtimeMetricRegistry = new MetricRegistry();
        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        ServiceLocatorUtilities.addOneConstant(serviceLocator, metricRegistry, "chatRegistry", MetricRegistry.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, metricRegistry, "runtimeRegistry", MetricRegistry.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, healthCheckRegistry);

        runtimeMetricRegistry.registerAll(new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
        runtimeMetricRegistry.registerAll(new GarbageCollectorMetricSet());
        runtimeMetricRegistry.registerAll(new JvmAttributeGaugeSet());
        runtimeMetricRegistry.registerAll(new MemoryUsageGaugeSet());
        runtimeMetricRegistry.register("socket.implementation", (Gauge<String>) () -> Epoll.isAvailable() ? "epoll" : "nio");
        runtimeMetricRegistry.register("ssl.implementation", (Gauge<String>) () -> OpenSsl.isAvailable() ? "openssl" : "java");
        runtimeMetricRegistry.register("ssl.cipherSuites", (Gauge<List<String>>) sslContext::cipherSuites);

        //database
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
        org.jooq.Configuration jooqConfiguration = new DefaultConfiguration();
        jooqConfiguration.set(dataSource);
        jooqConfiguration.set(SQLDialect.MYSQL);
        DSLContext dslContext = DSL.using(jooqConfiguration);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, dslContext, "dslContext", DSLContext.class);

        //proxy event loop
        EventLoopGroup proxyEventLoopGroup;
        if (Epoll.isAvailable()) {
            proxyEventLoopGroup = new EpollEventLoopGroup(1);
        } else {
            proxyEventLoopGroup = new NioEventLoopGroup(1);
        }
        ServiceLocatorUtilities.addOneConstant(serviceLocator, proxyEventLoopGroup, "proxyEventLoopGroup", EventLoopGroup.class);

        //frontend event loops
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

        ServiceLocatorUtilities.addOneConstant(serviceLocator, bossGroup, "frontend.bossLoopGroup", EventLoopGroup.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, childGroup, "frontend.childLoopGroup", EventLoopGroup.class);

        ThreadFactory scheduledThreadFactory = new ThreadFactoryBuilder().setNameFormat("SCHEDULER_%d").build();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(scheduledThreadFactory);

        AtomicLong messageId = new AtomicLong(0);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, messageId, "messageId", AtomicLong.class);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, scheduledExecutorService, "scheduler", ScheduledExecutorService.class);

        AuthenticationManager authenticationManager = serviceLocator.getService(AuthenticationManager.class);
        CaptchaService captchaService = serviceLocator.getService(CaptchaService.class);

        //todo: figure out a way to inject it automatically without circular dependency
        authenticationManager.registerAuthEventListener(serviceLocator.getService(TwitchCredentialsService.class));

        TicketService ticketService = serviceLocator.getService(TicketService.class);

        MessageConsumerServiceHandler messageConsumerServiceHandler = serviceLocator.getService(MessageConsumerServiceHandler.class);
        ProxyManager proxyManager = serviceLocator.getService(ProxyManager.class);
        messageConsumerServiceHandler.register(proxyManager);

        freemarker.template.Configuration freemarker = new freemarker.template.Configuration();
        freemarker.setClassForTemplateLoading(Main.class, "/templates");
        freemarker.setDefaultEncoding("UTF-8");
        freemarker.setObjectWrapper(new DefaultObjectWrapper());

        ReCaptcha reCaptcha = serviceLocator.getService(ReCaptcha.class);
        ViewResolvers viewResolvers = new ViewResolvers(freemarker);
        ServerMessageHandler serverMessageHandler = new ServerMessageHandler();

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
                property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
                register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(UserParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class);
                        bind(UserParamValueFactoryProvider.InjectionResolver.class).to(
                            new TypeLiteral<InjectionResolver<Auth>>() {
                            }).in(Singleton.class);
                    }
                });
                packages("lexek.wschat.frontend.http.rest");
            }
        };

        FileSystemStaticHandler fileSystemStaticHandler = new FileSystemStaticHandler(dataDir);
        fileSystemStaticHandler.addMaxAgeOverride("/emoticons/.*", (int) TimeUnit.DAYS.toSeconds(365));

        //todo: remove after migrating all this to jersey
        final RequestDispatcher httpRequestDispatcher = new RequestDispatcher(serverMessageHandler, viewResolvers, runtimeMetricRegistry, hostName);
        httpRequestDispatcher.add("/", new RedirectToAppHandler());
        httpRequestDispatcher.add("/.*", fileSystemStaticHandler);
        httpRequestDispatcher.add("/.*", new ClassPathStaticHandler(ClassPathStaticHandler.class, "/static/"));
        httpRequestDispatcher.add("/chat.html", new ChatHomeHandler(coreSettings.getTitle(), settings.getHttp().isAllowLikes(), settings.getHttp().isSingleRoom()));
        httpRequestDispatcher.add("/recaptcha/[0-9]+", new RecaptchaHandler(captchaService, reCaptcha));
        httpRequestDispatcher.add("/api/tickets", new UserTicketsHandler(authenticationManager, ticketService));
        httpRequestDispatcher.add("/admin/.*", new AdminPageHandler(authenticationManager));
        httpRequestDispatcher.add("/login", new LoginHandler(authenticationManager, reCaptcha));
        httpRequestDispatcher.add("/register", new RegistrationHandler(authenticationManager, reCaptcha));
        httpRequestDispatcher.add("/token", new TokenHandler(authenticationManager));

        JerseyContainer jerseyContainer = new JerseyContainer(authenticationManager, resourceConfig);
        ExtrasUtilities.bridgeServiceLocator(
            jerseyContainer.getApplicationHandler().getServiceLocator(),
            serviceLocator
        );
        httpRequestDispatcher.add("/rest/.*", jerseyContainer);

        HttpServer httpServer = new HttpServer(sslContext, httpRequestDispatcher);

        ServiceManager serviceManager = serviceLocator.getService(ServiceManager.class);
        serviceManager.registerService(httpServer);
        serviceManager.startAll();

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
