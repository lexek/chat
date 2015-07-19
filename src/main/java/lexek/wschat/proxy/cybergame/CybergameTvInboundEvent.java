package lexek.wschat.proxy.cybergame;

public class CybergameTvInboundEvent {

    private final CybergameTvEventType type;

    public CybergameTvInboundEvent(CybergameTvEventType type) {
        this.type = type;
    }

    public CybergameTvEventType getType() {
        return type;
    }
}
