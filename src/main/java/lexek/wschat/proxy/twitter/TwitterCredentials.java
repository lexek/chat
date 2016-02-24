package lexek.wschat.proxy.twitter;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TwitterCredentials {
    private final String consumerKey;
    private final String consumerSecret;
    private final String accessToken;
    private final String accessTokenSecret;

    public TwitterCredentials(
        @JsonProperty("consumerKey") String consumerKey,
        @JsonProperty("consumerSecret") String consumerSecret,
        @JsonProperty("accessToken") String accessToken,
        @JsonProperty("accessTokenSecret") String accessTokenSecret
    ) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.accessToken = accessToken;
        this.accessTokenSecret = accessTokenSecret;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }
}
