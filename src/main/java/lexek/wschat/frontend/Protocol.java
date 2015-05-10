package lexek.wschat.frontend;

public interface Protocol {
    Codec getCodec();

    boolean isNeedSendingBack();

    boolean isNeedNames();
}
