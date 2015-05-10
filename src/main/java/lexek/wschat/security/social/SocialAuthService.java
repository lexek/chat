package lexek.wschat.security.social;

import java.io.IOException;

public interface SocialAuthService {
    String getRedirectUrl();

    /*
     * @param authentication code
     * @return token
     */
    String authenticate(String code) throws IOException;

    SocialAuthProfile getProfile(String token) throws IOException;
}
