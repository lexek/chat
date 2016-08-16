package lexek.wschat.frontend.irc;

import lexek.wschat.frontend.Codec;
import lexek.wschat.frontend.Protocol;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class IrcProtocol implements Protocol {
    private final IrcCodec ircCodec;

    @Inject
    public IrcProtocol(IrcCodec ircCodec) {
        this.ircCodec = ircCodec;
    }

    @Override
    public Codec getCodec() {
        return ircCodec;
    }

    @Override
    public boolean isNeedSendingBack() {
        return false;
    }

    @Override
    public boolean isNeedNames() {
        return true;
    }
}
