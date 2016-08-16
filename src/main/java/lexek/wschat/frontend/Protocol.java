package lexek.wschat.frontend;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface Protocol {
    Codec getCodec();

    boolean isNeedSendingBack();

    boolean isNeedNames();
}
