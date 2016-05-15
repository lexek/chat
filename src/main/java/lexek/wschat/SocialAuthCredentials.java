package lexek.wschat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SocialAuthCredentials {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUrl;

    public SocialAuthCredentials(
        @JsonProperty("clientId") String clientId,
        @JsonProperty("clientSecret") String clientSecret,
        @JsonProperty("redirectUrl") String redirectUrl
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = redirectUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }
}
