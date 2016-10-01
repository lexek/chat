package lexek.wschat.chat;

import lexek.wschat.chat.model.ConnectionState;
import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.User;
import lexek.wschat.frontend.Codec;
import lexek.wschat.frontend.Protocol;

import java.net.InetAddress;

public abstract class Connection {
    private final Protocol protocol;
    private ConnectionState state;
    private User user;

    public Connection(Protocol protocol, User user) {
        this.protocol = protocol;
        this.user = user;
        this.state = ConnectionState.AUTHENTICATED;
    }

    protected Connection(Protocol protocol, ConnectionState initialState) {
        this.protocol = protocol;
        this.state = initialState;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        if (user == null) {
            throw new NullPointerException();
        }
        if (this.user != null || this.state != ConnectionState.AUTHENTICATING) {
            throw new IllegalStateException();
        }
        this.user = user;
        this.state = ConnectionState.AUTHENTICATED;
    }

    public ConnectionState getState() {
        return this.state;
    }

    public void setState(ConnectionState newState) {
        this.state = newState;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public Codec getCodec() {
        return protocol.getCodec();
    }

    public boolean isNeedNames() {
        return protocol.isNeedNames();
    }

    public boolean isNeedSendingBack() {
        return protocol.isNeedSendingBack();
    }

    @Override
    public String toString() {
        return "Connection{" +
            "protocol=" + protocol +
            ", user=" + user +
            ", state=" + state +
            '}';
    }


    public abstract String getIp();

    public abstract InetAddress getAddress();

    public abstract void send(Message message);

    public abstract void close();
}
