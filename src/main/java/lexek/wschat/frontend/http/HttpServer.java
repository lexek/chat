package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import lexek.httpserver.RequestDispatcher;
import lexek.wschat.services.AbstractService;
import lexek.wschat.util.ExceptionLogger;

import javax.net.ssl.SSLException;
import java.io.FileNotFoundException;
import java.util.concurrent.ThreadFactory;

public class HttpServer extends AbstractService<Integer> {
    private static final int PORT = 1337;
    private ServerBootstrap bootstrap;
    private Channel channel;

    public HttpServer(SslContext sslContext, RequestDispatcher requestDispatcher)
        throws FileNotFoundException, SSLException {
        super("httpServer", ImmutableList.<String>of());
        this.stateData = PORT;

        EventLoopGroup parentGroup;
        EventLoopGroup childGroup;
        ThreadFactory bossThreadFactory = new ThreadFactoryBuilder().setNameFormat("HTTP_BOSS_THREAD_%d").build();
        ThreadFactory childThreadFactory = new ThreadFactoryBuilder().setNameFormat("HTTP_CHILD_THREAD_%d").build();
        if (Epoll.isAvailable()) {
            logger.debug("Using epoll");
            parentGroup = new EpollEventLoopGroup(1, bossThreadFactory);
            childGroup = new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors(), childThreadFactory);
        } else {
            logger.debug("Using nio");
            parentGroup = new NioEventLoopGroup(1, bossThreadFactory);
            childGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), childThreadFactory);
        }

        this.bootstrap = new ServerBootstrap();
        this.bootstrap.group(parentGroup, childGroup);
        if (Epoll.isAvailable()) {
            this.bootstrap.channel(EpollServerSocketChannel.class);
        } else {
            this.bootstrap.channel(NioServerSocketChannel.class);
        }
        this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        this.bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        ExceptionLogger exceptionLogger = new ExceptionLogger();
        this.bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(sslContext.newHandler(ByteBufAllocator.DEFAULT));
                pipeline.addLast("decompressor", new HttpContentDecompressor());
                pipeline.addLast("codec", new HttpServerCodec(4096, 8192, 8192));
                pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast("compressor", new HttpContentCompressor());
                pipeline.addLast("handler", requestDispatcher);
                pipeline.addLast(exceptionLogger);
            }
        });
    }

    @Override
    public void performAction(String action) {
    }

    @Override
    public void stop() {
        if (this.channel.isOpen()) {
            this.channel.close();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    protected void start0() {
        this.channel = bootstrap.bind(PORT).awaitUninterruptibly().channel();
    }
}
