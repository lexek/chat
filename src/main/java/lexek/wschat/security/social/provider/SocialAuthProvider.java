package lexek.wschat.security.social.provider;

import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.security.social.SocialRedirect;
import lexek.wschat.security.social.SocialToken;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

public interface SocialAuthProvider {
    SocialRedirect getRedirect() throws IOException;

    boolean validateParams(MultivaluedMap<String, String> params);

    boolean validateState(MultivaluedMap<String, String> params, String cookieState);

    SocialToken authenticate(MultivaluedMap<String, String> params) throws IOException;

    SocialProfile getProfile(SocialToken token) throws IOException;

    SocialToken refresh(SocialToken token) throws IOException;

    boolean needsRefreshing();

    boolean checkEmail();

    String getName();

    String getUrl();
}
