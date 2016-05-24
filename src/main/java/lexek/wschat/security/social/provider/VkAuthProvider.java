package lexek.wschat.security.social.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.google.common.net.UrlEscapers;
import lexek.wschat.chat.e.BadRequestException;
import lexek.wschat.security.SecureTokenGenerator;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.security.social.SocialRedirect;
import lexek.wschat.security.social.SocialToken;
import lexek.wschat.util.JsonResponseHandler;
import lexek.wschat.util.RestResponse;
import lexek.wschat.util.RestResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class VkAuthProvider implements SocialAuthProvider {
    private final String name;
    private final String clientId;
    private final String clientSecret;
    private final String url;
    private final HttpClient httpClient;
    private final SecureTokenGenerator secureTokenGenerator;

    public VkAuthProvider(
        String clientId,
        String clientSecret,
        String url,
        String name,
        HttpClient httpClient,
        SecureTokenGenerator secureTokenGenerator
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.url = url;
        this.name = name;
        this.httpClient = httpClient;
        this.secureTokenGenerator = secureTokenGenerator;
    }

    @Override
    public SocialRedirect getRedirect() {
        String state = secureTokenGenerator.generateRandomToken(32).replace("=", "");
        String result = "https://oauth.vk.com/authorize?response_type=code" +
            "&client_id=" + UrlEscapers.urlPathSegmentEscaper().escape(clientId) +
            "&redirect_uri=" + UrlEscapers.urlPathSegmentEscaper().escape(url) +
            "&display=page" +
            "&state=" + UrlEscapers.urlPathSegmentEscaper().escape(state) +
            "&prompt=consent" +
            "&access_type=offline";
        return new SocialRedirect(result, state);
    }

    @Override
    public SocialToken authenticate(String code) throws IOException {
        String tokenUrl = "https://oauth.vk.com/access_token" +
            "?client_id=" + UrlEscapers.urlPathSegmentEscaper().escape(clientId) +
            "&client_secret=" + UrlEscapers.urlPathSegmentEscaper().escape(clientSecret) +
            "&redirect_uri=" + UrlEscapers.urlPathSegmentEscaper().escape(url) +
            "&code=" + UrlEscapers.urlPathSegmentEscaper().escape(code);
        HttpGet request = new HttpGet(tokenUrl);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        RestResponse response = httpClient.execute(request, RestResponseHandler.INSTANCE);
        JsonNode rootNode = response.getRootNode();
        if (response.isSuccess()) {
            String token = rootNode.get("access_token").asText();
            long expiresIn = TimeUnit.SECONDS.toMillis(rootNode.get("expires_in").asLong() - 60);
            return new SocialToken(
                name,
                token,
                System.currentTimeMillis() + expiresIn,
                null
            );
        } else {
            throw new BadRequestException(rootNode.get("error_description").asText());
        }
    }

    @Override
    public SocialProfile getProfile(SocialToken token) throws IOException {
        String profileUrl = "https://api.vk.com/method/users.get" +
            "?fields=name,last_name,first_name" +
            "&access_token=" + token.getToken();
        HttpGet request = new HttpGet(profileUrl);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.getToken());
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode users = httpClient.execute(request, JsonResponseHandler.INSTANCE).get("response");
        JsonNode userNode = users.get(0);
        String firstName = userNode.get("first_name").asText();
        String lastName = userNode.get("last_name").asText();
        String externalName = firstName + " " + lastName;
        String externalId = userNode.get("uid").asText();
        return new SocialProfile(externalId, name, externalName, null, token);
    }

    @Override
    public SocialToken refresh(SocialToken oldToken) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean needsRefreshing() {
        return false;
    }

    @Override
    public boolean checkEmail() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.OAUTH_2;
    }
}
