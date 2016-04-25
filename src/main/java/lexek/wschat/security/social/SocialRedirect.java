package lexek.wschat.security.social;

public class SocialRedirect {
    private final String url;
    private final String state;

    public SocialRedirect(String url, String state) {
        this.url = url;
        this.state = state;
    }

    public String getUrl() {
        return url;
    }

    public String getState() {
        return state;
    }
}
