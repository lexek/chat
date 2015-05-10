package lexek.wschat.proxy.cybergame;

public class CybergameTvInboundMessage extends CybergameTvInboundEvent {
    private final String from;
    private final String text;

    public CybergameTvInboundMessage(String from, String text) {
        super(CybergameTvEventType.MESSAGE);
        this.from = from;
        this.text = text;
    }

    public String getFrom() {
        return from;
    }

    public String getText() {
        return text;
    }
}
