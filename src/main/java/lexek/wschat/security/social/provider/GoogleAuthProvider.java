package lexek.wschat.security.social.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import com.google.common.net.UrlEscapers;
import io.netty.util.CharsetUtil;
import lexek.wschat.chat.e.BadRequestException;
import lexek.wschat.security.SecureTokenGenerator;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.security.social.SocialRedirect;
import lexek.wschat.security.social.SocialToken;
import lexek.wschat.util.JsonResponseHandler;
import lexek.wschat.util.RestResponse;
import lexek.wschat.util.RestResponseHandler;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GoogleAuthProvider implements SocialAuthProvider {
    private final String name;
    private final String clientId;
    private final String clientSecret;
    private final String url;
    private final HttpClient httpClient;
    private final SecureTokenGenerator secureTokenGenerator;
    private final String scopesString;

    public GoogleAuthProvider(
        String clientId,
        String clientSecret,
        String url,
        Set<String> scopes,
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
        this.scopesString = scopes.stream().collect(Collectors.joining(" "));
    }

    @Override
    public SocialRedirect getRedirect() {
        String state = secureTokenGenerator.generateRandomToken(32).replace("=", "");
        String result = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=" + clientId +
            "&redirect_uri=" + UrlEscapers.urlPathSegmentEscaper().escape(url) +
            "&scope=" + UrlEscapers.urlPathSegmentEscaper().escape(scopesString) +
            "&state=" + UrlEscapers.urlPathSegmentEscaper().escape(state) +
            "&prompt=consent" +
            "&access_type=offline";
        return new SocialRedirect(result, state);
    }

    @Override
    public SocialToken authenticate(String code) throws IOException {
        HttpPost request = new HttpPost("https://www.googleapis.com/oauth2/v4/token");
        HttpEntity entity = new UrlEncodedFormEntity(ImmutableList.of(
            new BasicNameValuePair("client_secret", clientSecret),
            new BasicNameValuePair("client_id", clientId),
            new BasicNameValuePair("grant_type", "authorization_code"),
            new BasicNameValuePair("redirect_uri", url),
            new BasicNameValuePair("code", code)
        ), CharsetUtil.UTF_8);
        request.setEntity(entity);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        RestResponse response = httpClient.execute(request, RestResponseHandler.INSTANCE);
        JsonNode rootNode = response.getRootNode();
        if (response.isSuccess()) {
            String token = rootNode.get("access_token").asText();
            String refreshToken = rootNode.get("refresh_token").asText();
            long expiresIn = TimeUnit.SECONDS.toMillis(rootNode.get("expires_in").asLong() - 60);
            return new SocialToken(
                name,
                token,
                System.currentTimeMillis() + expiresIn,
                refreshToken
            );
        } else {
            throw new BadRequestException(rootNode.get("error_description").asText());
        }
    }

    @Override
    public SocialProfile getProfile(SocialToken token) throws IOException {
        HttpGet request = new HttpGet("https://www.googleapis.com/plus/v1/people/me");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.getToken());
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode userData = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        String externalName = userData.get("displayName").asText().toLowerCase();
        String externalId = userData.get("id").asText();
        String email = userData.get("emails").get(0).get("value").asText();
        return new SocialProfile(externalId, name, externalName, email, token);
    }

    @Override
    public SocialToken refresh(SocialToken oldToken) throws IOException {
        HttpPost request = new HttpPost("https://www.googleapis.com/oauth2/v4/token");
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setEntity(new UrlEncodedFormEntity(ImmutableList.of(
            new BasicNameValuePair("client_id", clientId),
            new BasicNameValuePair("client_secret", clientSecret),
            new BasicNameValuePair("refresh_token", oldToken.getRefreshToken()),
            new BasicNameValuePair("grant_type", "refresh_token")
        )));
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        String token = response.get("access_token").asText();
        long expiresIn = TimeUnit.SECONDS.toMillis(response.get("expires_in").asLong() - 60);
        return new SocialToken(
            name,
            token,
            System.currentTimeMillis() + expiresIn,
            oldToken.getRefreshToken()
        );
    }

    @Override
    public boolean needsRefreshing() {
        return true;
    }

    @Override
    public boolean checkEmail() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUrl() {
        return url;
    }
}
