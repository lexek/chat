package lexek.wschat.frontend.ws;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.model.ConnectionState;
import lexek.wschat.chat.model.Message;
import lexek.wschat.util.Net;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class WebSocketConnectionAdapter extends Connection {
    private final Channel channel;

    public WebSocketConnectionAdapter(Channel channel, WebSocketProtocol protocol) {
        super(protocol, ConnectionState.CONNECTED);
        this.channel = channel;
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
        channel.writeAndFlush(new TextWebSocketFrame(getCodec().encode(message)));
    }

    public void send(String message) {
        channel.writeAndFlush(new TextWebSocketFrame(message));
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
