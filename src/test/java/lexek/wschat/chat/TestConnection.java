package lexek.wschat.chat;

import lexek.wschat.chat.model.Message;
import lexek.wschat.chat.model.User;

import java.net.InetAddress;

public class TestConnection extends Connection {
    public TestConnection(User user) {
        super(null, user);
    }

    @Override
    public String getIp() {
        return "127.0.0.1";
    }

    @Override
    public InetAddress getAddress() {
        return null;
    }

    @Override
    public void send(Message message) {

    }

    @Override
    public void close() {

    }
}
