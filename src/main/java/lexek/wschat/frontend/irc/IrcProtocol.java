package lexek.wschat.frontend.irc;

import lexek.wschat.frontend.Protocol;
import org.jvnet.hk2.annotations.Service;

@Service
public class IrcProtocol implements Protocol {
    @Override
    public boolean isNeedSendingBack() {
        return false;
    }

    @Override
    public boolean isNeedNames() {
        return true;
    }
}
