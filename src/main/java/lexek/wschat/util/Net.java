package lexek.wschat.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class Net {

    private Net() {
    }

    public static String getIp(SocketAddress address) {
        return ((InetSocketAddress) address).getAddress().getHostAddress();
    }
}
