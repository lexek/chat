package lexek.wschat.frontend.ws;

import lexek.wschat.frontend.Codec;
import lexek.wschat.frontend.Protocol;

public class WebSocketProtocol implements Protocol {
    private final JsonCodec codec = new JsonCodec();

    @Override
    public Codec getCodec() {
        return codec;
    }

    @Override
    public boolean isNeedSendingBack() {
        return true;
    }

    @Override
    public boolean isNeedNames() {
        return false;
    }
}
