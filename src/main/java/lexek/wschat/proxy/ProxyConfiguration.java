package lexek.wschat.proxy;

public class ProxyConfiguration {
    private final String channel;
    private final String type;
    private final boolean useTitle;

    public ProxyConfiguration(String channel, String type, boolean useTitle) {
        this.channel = channel;
        this.type = type;
        this.useTitle = useTitle;
    }

    public String getChannel() {
        return channel;
    }

    public String getType() {
        return type;
    }

    public boolean isUseTitle() {
        return useTitle;
    }
}
