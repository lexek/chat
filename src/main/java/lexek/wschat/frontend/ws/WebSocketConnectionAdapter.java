package lexek.wschat.frontend.ws;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.model.ConnectionState;
import lexek.wschat.chat.model.Message;
import lexek.wschat.util.Net;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class WebSocketConnectionAdapter extends Connection {
    private final Channel channel;
    private final JsonCodec codec;

    public WebSocketConnectionAdapter(Channel channel, WebSocketProtocol protocol, JsonCodec codec) {
        super(protocol, ConnectionState.CONNECTED);
        this.channel = channel;
        this.codec = codec;
    }

    @Override
    public String getIp() {
        return Net.getIp(channel.remoteAddress());
    }

    @Override
    public InetAddress getAddress() {
        return ((InetSocketAddress) channel.remoteAddress()).getAddress();
    }

    @Override
    public void send(Message message) {
        channel.writeAndFlush(codec.encode(message), channel.voidPromise());
    }

    public void send(WebSocketFrame frame) {
        channel.writeAndFlush(frame, channel.voidPromise());
    }

    @Override
    public void close() {
        channel.close();
    }

    @Override
    public String toString() {
        return "WebSocketConnectionAdapter{" +
            "channel=" + channel.metadata() +
            "} " + super.toString();
    }

    public Channel getChannel() {
        return this.channel;
    }
}
