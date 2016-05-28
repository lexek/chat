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
import java.util.concurrent.TimeUnit;

public class GoodGameAuthProvider extends AbstractOauth2Provider {
    private final String name;
    private final String clientId;
    private final String clientSecret;
    private final String url;
    private final HttpClient httpClient;
    private final SecureTokenGenerator secureTokenGenerator;

    public GoodGameAuthProvider(
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
        String result = "http://api2.goodgame.ru/oauth/authorize?response_type=code" +
            "&client_id=" + UrlEscapers.urlPathSegmentEscaper().escape(clientId) +
            "&redirect_uri=" + UrlEscapers.urlPathSegmentEscaper().escape(url) +
            "&scope=chat.token" +
            "&state=" + UrlEscapers.urlPathSegmentEscaper().escape(state) +
            "&prompt=consent" +
            "&access_type=offline";
        return new SocialRedirect(result, state);
    }

    @Override
    protected SocialToken authenticate(String code) throws IOException {
        HttpPost request = new HttpPost("http://api2.goodgame.ru/oauth");
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
            throw new BadRequestException(rootNode.get("detail").asText());
        }
    }

    @Override
    public SocialProfile getProfile(SocialToken token) throws IOException {
        HttpGet request = new HttpGet("http://api2.goodgame.ru/info");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.getToken());
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode userData = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        JsonNode userNode = userData.get("user");
        String externalName = userNode.get("username").asText().toLowerCase();
        String externalId = userNode.get("user_id").asText();
        return new SocialProfile(externalId, name, externalName, null, token);
    }

    @Override
    public SocialToken refresh(SocialToken oldToken) throws IOException {
        HttpPost request = new HttpPost("http://api2.goodgame.ru/oauth");
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setEntity(new UrlEncodedFormEntity(ImmutableList.of(
            new BasicNameValuePair("client_id", clientId),
            new BasicNameValuePair("client_secret", clientSecret),
            new BasicNameValuePair("refresh_token", oldToken.getRefreshToken()),
            new BasicNameValuePair("grant_type", "refresh_token")
        )));
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        String token = response.get("access_token").asText();
        String refreshToken = response.get("refresh_token").asText();
        long expiresIn = TimeUnit.SECONDS.toMillis(response.get("expires_in").asLong() - 60);
        return new SocialToken(
            name,
            token,
            System.currentTimeMillis() + expiresIn,
            refreshToken
        );
    }

    @Override
    public boolean needsRefreshing() {
        return true;
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
}
