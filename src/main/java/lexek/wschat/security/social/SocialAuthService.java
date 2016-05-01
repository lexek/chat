package lexek.wschat.security.social;

import java.io.IOException;

public interface SocialAuthService {
    SocialRedirect getRedirect();

    SocialToken authenticate(String code) throws IOException;

    SocialProfile getProfile(SocialToken token) throws IOException;

    SocialToken refresh(SocialToken token) throws IOException;

    boolean needsRefreshing();

    String getName();

    String getUrl();
}
