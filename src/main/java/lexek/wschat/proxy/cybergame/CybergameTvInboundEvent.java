package lexek.wschat.proxy.cybergame;

public class CybergameTvInboundEvent {

    public CybergameTvInboundEvent(CybergameTvEventType type) {
        this.type = type;
    }

    public CybergameTvEventType getType() {
        return type;
    }

    private final CybergameTvEventType type;
}
