package lexek.wschat.security.social;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SocialAuthCredentials {
    private final String clientId;
    private final String clientSecret;

    public SocialAuthCredentials(
        @JsonProperty("clientId") String clientId,
        @JsonProperty("clientSecret") String clientSecret
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
