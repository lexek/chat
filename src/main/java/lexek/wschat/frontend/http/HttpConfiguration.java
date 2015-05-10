package lexek.wschat.frontend.http;

public class HttpConfiguration {
    private final int port;
    private final String recaptchaKey;
    private final String recaptchaPubKey;
    private final String twitchClientId;
    private final String twitchSecret;
    private final String twitchUrl;
    private final boolean allowLikes;
    private final boolean singleRoom;

    public HttpConfiguration(int port, String recaptchaKey, String recaptchaPubKey, String twitchClientId, String twitchSecret, String twitchUrl, boolean allowLikes, boolean singleRoom) {
        this.port = port;
        this.recaptchaKey = recaptchaKey;
        this.recaptchaPubKey = recaptchaPubKey;
        this.twitchClientId = twitchClientId;
        this.twitchSecret = twitchSecret;
        this.twitchUrl = twitchUrl;
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

    public String getTwitchClientId() {
        return twitchClientId;
    }

    public String getTwitchSecret() {
        return twitchSecret;
    }

    public String getTwitchUrl() {
        return twitchUrl;
    }

    public boolean isAllowLikes() {
        return allowLikes;
    }

    public boolean isSingleRoom() {
        return singleRoom;
    }
}
