package lexek.httpserver;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AsciiString;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.util.BufferInputStream;
import lexek.wschat.util.BufferOutputStream;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.*;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.Application;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class JerseyContainer extends SimpleHttpHandler implements Container {
    private static final DateTimeFormatter apacheDateFormatter = DateTimeFormatter
        .ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
        .withLocale(Locale.US);

    private final Type RequestType = (new TypeLiteral<Ref<Request>>() {}).getType();
    private final Type ResponseType = (new TypeLiteral<Ref<Response>>() {}).getType();

    private final Logger logger = LoggerFactory.getLogger(JerseyContainer.class);
    private final Logger accessLogger = LoggerFactory.getLogger("access");
    private final AuthenticationManager authenticationManager;
    private final ApplicationHandler applicationHandler;

    private static class JerseyRequestReferencingFactory extends ReferencingFactory<Request> {
        @Inject
        public JerseyRequestReferencingFactory(final Provider<Ref<Request>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class JerseyResponseReferencingFactory extends ReferencingFactory<Response> {
        @Inject
        public JerseyResponseReferencingFactory(final Provider<Ref<Response>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class RequestResponseBinder extends AbstractBinder {
        @Override
        protected void configure() {
            bindFactory(JerseyRequestReferencingFactory.class).to(Request.class)
                .proxy(false).in(RequestScoped.class);
            bindFactory(ReferencingFactory.<Request>referenceFactory()).to(new TypeLiteral<Ref<Request>>() {})
                .in(RequestScoped.class);

            bindFactory(JerseyResponseReferencingFactory.class).to(Response.class)
                .proxy(true).proxyForSameScope(false).in(RequestScoped.class);
            bindFactory(ReferencingFactory.<Response>referenceFactory()).to(new TypeLiteral<Ref<Response>>() {})
                .in(RequestScoped.class);
        }
    }

    public JerseyContainer(AuthenticationManager authenticationManager, Application resourceConfig) {
        this.authenticationManager = authenticationManager;
        this.applicationHandler = new ApplicationHandler(resourceConfig, new RequestResponseBinder());
    }

    @Override
    public ResourceConfig getConfiguration() {
        return applicationHandler.getConfiguration();
    }

    @Override
    public ApplicationHandler getApplicationHandler() {
        return applicationHandler;
    }

    @Override
    public void reload() {

    }

    @Override
    public void reload(ResourceConfig resourceConfig) {

    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        final String hostHeader = request.header(HttpHeaderNames.HOST);
        URI baseUri = new URI("https://" + hostHeader + "/rest/");
        URI requestUri = new URI("https://" + hostHeader + request.uri());
        UserDto userDto = authenticationManager.checkFullAuthentication(request);
        ContainerRequest containerRequest = new ContainerRequest(
            baseUri,
            requestUri,
            request.method().name(),
            new JerseySecurityContext(userDto),
            new MapPropertiesDelegate()
        );
        ResponseWriter responseWriter = new ResponseWriter(request, response, userDto);
        containerRequest.setEntityStream(new BufferInputStream(request.content()));
        request.headers().forEach(e -> containerRequest.getHeaders().putSingle(e.getKey(), e.getValue()));
        containerRequest.setWriter(responseWriter);
        containerRequest.setRequestScopedInitializer(locator -> {
            locator.<Ref<Request>>getService(RequestType).set(request);
            locator.<Ref<Response>>getService(ResponseType).set(response);
        });
        try {
            applicationHandler.handle(containerRequest);
        } catch (Exception e) {
            logger.error("exception occured", e);
        }
    }

    private class ResponseWriter implements ContainerResponseWriter {
        private final Request request;
        private final Response response;
        private final UserDto userDto;

        private ResponseWriter(Request request, Response response, UserDto userDto) {
            this.request = request;
            this.response = response;
            this.userDto = userDto;
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext) throws ContainerException {
            responseContext
                .getHeaders()
                .forEach((k, v) ->
                    v.forEach(e ->
                        response.header(AsciiString.of(k), e.toString())
                    )
                );
            response.status(responseContext.getStatus());
            return new BufferOutputStream(response.getBuffer());
        }

        @Override
        public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
            return false;
        }

        @Override
        public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {

        }

        @Override
        public void commit() {
            int responseSize = response.size();
            accessLogger.info(
                "{} {} - [{}] \"{} {} HTTP/1.1\" {} {} \"{}\" \"{}\"",
                request.ip(),
                userDto != null ? userDto.getName() : "-",
                ZonedDateTime.now().format(apacheDateFormatter),
                request.method().name(),
                request.uri(),
                response.status(),
                responseSize != 0 ? responseSize : "-",
                request.header(HttpHeaderNames.REFERER),
                request.header(HttpHeaderNames.USER_AGENT)
            );
        }

        @Override
        public void failure(Throwable error) {
            logger.warn("exception in writer", error);
            response.internalError();
        }

        @Override
        public boolean enableResponseBuffering() {
            return false;
        }
    }
}
