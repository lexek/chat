package lexek.wschat.security.social.provider;

import lexek.wschat.security.social.SocialToken;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

public abstract class AbstractOauth2Provider implements SocialAuthProvider {
    @Override
    public boolean validateParams(MultivaluedMap<String, String> params) {
        return params.getFirst("code") != null;
    }

    @Override
    public boolean validateState(MultivaluedMap<String, String> params, String cookieState) {
        return cookieState == null || cookieState.equals(params.getFirst("state"));
    }

    @Override
    public SocialToken authenticate(MultivaluedMap<String, String> params) throws IOException {
        return authenticate(params.getFirst("code"));
    }

    protected abstract SocialToken authenticate(String code) throws IOException;
}
