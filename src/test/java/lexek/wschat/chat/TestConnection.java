package lexek.wschat.chat;

import lexek.wschat.frontend.Protocol;

public class TestConnection extends Connection {
    public TestConnection(User user) {
        super(null, user);
    }

    @Override
    public String getIp() {
        return "127.0.0.1";
    }

    @Override
    public void send(Message message) {

    }

    @Override
    public void close() {

    }
}
