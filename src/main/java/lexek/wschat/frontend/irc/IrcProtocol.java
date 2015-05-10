package lexek.wschat.frontend.irc;

import lexek.wschat.frontend.Codec;
import lexek.wschat.frontend.Protocol;

public class IrcProtocol implements Protocol {
    private final IrcCodec ircCodec;

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
