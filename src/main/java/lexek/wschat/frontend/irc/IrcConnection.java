package lexek.wschat.frontend.irc;

import io.netty.channel.Channel;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.model.ConnectionState;
import lexek.wschat.chat.model.Message;
import lexek.wschat.util.Net;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class IrcConnection extends Connection {
    private final Channel channel;
    private String password;

    public IrcConnection(IrcProtocol protocol, Channel channel) {
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
        send(getCodec().encode(message));
    }

    public void send(String message) {
        if (message != null) {
            channel.writeAndFlush(message + "\r\n");
        }
    }

    @Override
    public void close() {
        channel.close();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "IrcConnection{" +
            "channel=" + channel +
            "} " + super.toString();
    }
}
