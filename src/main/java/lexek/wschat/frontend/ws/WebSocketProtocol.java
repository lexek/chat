package lexek.wschat.frontend.ws;

import lexek.wschat.frontend.Protocol;
import org.jvnet.hk2.annotations.Service;

@Service
public class WebSocketProtocol implements Protocol {
    @Override
    public boolean isNeedSendingBack() {
        return true;
    }

    @Override
    public boolean isNeedNames() {
        return false;
    }
}
