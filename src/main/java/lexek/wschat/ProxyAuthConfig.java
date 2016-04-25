package lexek.wschat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProxyAuthConfig {
    private final ProxyAuthCredentials google;
    private final ProxyAuthCredentials twitch;

    public ProxyAuthConfig(
        @JsonProperty("google") ProxyAuthCredentials google,
        @JsonProperty("twitch") ProxyAuthCredentials twitch
    ) {
        this.google = google;
        this.twitch = twitch;
    }

    public ProxyAuthCredentials getGoogle() {
        return google;
    }

    public ProxyAuthCredentials getTwitch() {
        return twitch;
    }
}
