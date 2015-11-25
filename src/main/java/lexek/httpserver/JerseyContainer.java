package lexek.httpserver;

import io.netty.handler.codec.http.HttpHeaders;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.security.AuthenticationManager;
import lexek.wschat.util.BufferInputStream;
import lexek.wschat.util.BufferOutputStream;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.*;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class JerseyContainer extends SimpleHttpHandler implements Container {
    private final Logger logger = LoggerFactory.getLogger(JerseyContainer.class);
    private final AuthenticationManager authenticationManager;
    private final ApplicationHandler applicationHandler;

    public JerseyContainer(AuthenticationManager authenticationManager, ApplicationHandler applicationHandler) {
        this.authenticationManager = authenticationManager;
        this.applicationHandler = applicationHandler;
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
        final String hostHeader = request.header(HttpHeaders.Names.HOST);
        URI baseUri = new URI("https://" + hostHeader + "/rest/");
        URI requestUri = new URI("https://" + hostHeader + request.uri());
        UserDto userDto = authenticationManager.checkAuthentication(request);
        ContainerRequest containerRequest = new ContainerRequest(
            baseUri, requestUri, request.method().name(), new JerseySecurityContext(userDto), new MapPropertiesDelegate()
        );
        ResponseWriter responseWriter = new ResponseWriter(response);
        containerRequest.setEntityStream(new BufferInputStream(request.content()));
        request.headers().forEach(e -> containerRequest.getHeaders().putSingle(e.getKey(), e.getValue()));
        containerRequest.setWriter(responseWriter);
        try {
            applicationHandler.handle(containerRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ResponseWriter implements ContainerResponseWriter {
        private final Response response;

        private ResponseWriter(Response response) {
            this.response = response;
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext) throws ContainerException {
            responseContext.getHeaders().forEach((k, v) -> v.forEach(e -> response.header(k, e.toString())));
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
