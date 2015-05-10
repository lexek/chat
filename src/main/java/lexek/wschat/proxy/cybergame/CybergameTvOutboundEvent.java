package lexek.wschat.proxy.cybergame;

public class CybergameTvOutboundEvent {
    private final CybergameTvEventType type;
    private final String channel;

    public CybergameTvOutboundEvent(CybergameTvEventType type, String channel) {
        this.type = type;
        this.channel = channel;
    }

    public CybergameTvEventType getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }
}
