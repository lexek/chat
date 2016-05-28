package lexek.wschat.frontend.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HttpConfiguration {
    private final int port;
    private final String recaptchaKey;
    private final String recaptchaPubKey;

    private final boolean allowLikes;
    private final boolean singleRoom;

    public HttpConfiguration(@JsonProperty("port") int port,
                             @JsonProperty("recaptchaKey") String recaptchaKey,
                             @JsonProperty("recaptchaPubKey") String recaptchaPubKey,
                             @JsonProperty("allowLikes") boolean allowLikes,
                             @JsonProperty("singleRoom") boolean singleRoom) {
        this.port = port;
        this.recaptchaKey = recaptchaKey;
        this.recaptchaPubKey = recaptchaPubKey;
        this.allowLikes = allowLikes;
        this.singleRoom = singleRoom;
    }

    public int getPort() {
        return port;
    }

    public String getRecaptchaKey() {
        return recaptchaKey;
    }

    public String getRecaptchaPubKey() {
        return recaptchaPubKey;
    }

    public boolean isAllowLikes() {
        return allowLikes;
    }

    public boolean isSingleRoom() {
        return singleRoom;
    }
}
