package lexek.wschat.proxy;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProxyConfiguration {
    private final String channel;
    private final String type;
    private final boolean useTitle;

    public ProxyConfiguration(@JsonProperty("channel") String channel,
                              @JsonProperty("type") String type,
                              @JsonProperty("useTitle") boolean useTitle) {
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
