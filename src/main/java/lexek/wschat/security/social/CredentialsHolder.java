package lexek.wschat.security.social;

import java.util.Map;

public class CredentialsHolder {
    private final Map<String, SocialAuthCredentials> data;

    public CredentialsHolder(Map<String, SocialAuthCredentials> data) {
        this.data = data;
    }

    public Map<String, SocialAuthCredentials> get() {
        return data;
    }
}
