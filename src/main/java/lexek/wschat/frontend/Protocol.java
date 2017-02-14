package lexek.wschat.frontend;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface Protocol {
    boolean isNeedSendingBack();

    boolean isNeedNames();
}
