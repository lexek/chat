package lexek.wschat.security.social;

public class SessionResult {
    private final SocialAuthResultType type;
    private final String sessionId;

    public SessionResult(SocialAuthResultType type, String sessionId) {
        this.type = type;
        this.sessionId = sessionId;
    }

    public SocialAuthResultType getType() {
        return type;
    }

    public String getSessionId() {
        return sessionId;
    }
}
