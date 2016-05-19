package lexek.wschat.security.social;

import java.io.IOException;

public interface SocialAuthProvider {
    SocialRedirect getRedirect();

    default SocialToken authenticate(String token, String verifier) throws IOException {
        throw new UnsupportedOperationException();
    }

    SocialToken authenticate(String code) throws IOException;

    SocialProfile getProfile(SocialToken token) throws IOException;

    SocialToken refresh(SocialToken token) throws IOException;

    boolean needsRefreshing();

    boolean checkEmail();

    String getName();

    String getUrl();

    default boolean isV1() {
        return false;
    }
}