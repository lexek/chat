package lexek.wschat.security.social;

public class SocialToken {
    private final String service;
    private final String token;
    private final Long expires;
    private final String refreshToken;

    public SocialToken(String service, String token, Long expires, String refreshToken) {
        this.service = service;
        this.token = token;
        this.expires = expires;
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public Long getExpires() {
        return expires;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getService() {
        return service;
    }
}
