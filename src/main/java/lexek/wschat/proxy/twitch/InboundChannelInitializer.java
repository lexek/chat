package lexek.wschat.proxy.twitch;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

public class InboundChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final ChannelHandler stringEncoder = new StringEncoder(CharsetUtil.UTF_8);
    private final ChannelHandler stringDecoder = new StringDecoder(CharsetUtil.UTF_8);
    private final JTVEventListener eventListener;
    private final String remoteRoom;
    private final String username;
    private final String token;

    public InboundChannelInitializer(
        JTVEventListener eventListener,
        String remoteRoom,
        String username,
        String token
    ) {
        this.eventListener = eventListener;
        this.remoteRoom = remoteRoom;
        this.username = username;
        this.token = token;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast(stringEncoder);
        pipeline.addLast(stringDecoder);
        pipeline.addLast(new IdleStateHandler(120, 0, 140, TimeUnit.SECONDS));
        pipeline.addLast(new TwitchTvMessageDecoder());
        pipeline.addLast(new TwitchMessageHandler(eventListener, remoteRoom, username, token));
    }
}
