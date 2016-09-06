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

public class StreamLabsProvider extends AbstractOauth2Provider {
    private final String name;
    private final String clientId;
    private final String secret;
    private final String url;
    private final HttpClient httpClient;
    private final SecureTokenGenerator secureTokenGenerator;

    public StreamLabsProvider(
        String clientId,
        String secret,
        String url,
        String name,
        HttpClient httpClient,
        SecureTokenGenerator secureTokenGenerator
    ) {
        this.clientId = clientId;
        this.secret = secret;
        this.url = url;
        this.name = name;
        this.httpClient = httpClient;
        this.secureTokenGenerator = secureTokenGenerator;
    }

    @Override
    public SocialRedirect getRedirect() {
        String state = secureTokenGenerator.generateRandomToken(32).replace("=", "");
        String result = "https://streamlabs.com/api/v1.0/authorize?response_type=code&client_id=" + clientId +
            "&redirect_uri=" + UrlEscapers.urlPathSegmentEscaper().escape(url) +
            "&scope=donations.read" +
            "&state=" + UrlEscapers.urlPathSegmentEscaper().escape(state);
        return new SocialRedirect(result, state);
    }

    @Override
    protected SocialToken authenticate(String code) throws IOException {
        HttpPost request = new HttpPost("https://streamlabs.com/api/v1.0/token");
        HttpEntity entity = new UrlEncodedFormEntity(ImmutableList.of(
            new BasicNameValuePair("client_secret", secret),
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
            return new SocialToken(
                name,
                rootNode.get("access_token").asText(),
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(55),
                rootNode.get("refresh_token").asText()
            );
        } else {
            throw new BadRequestException(rootNode.get("message").asText());
        }
    }

    @Override
    public SocialToken refresh(SocialToken oldToken) throws IOException {
        HttpPost request = new HttpPost("https://streamlabs.com/api/v1.0/token");
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setEntity(new UrlEncodedFormEntity(ImmutableList.of(
            new BasicNameValuePair("client_id", clientId),
            new BasicNameValuePair("client_secret", secret),
            new BasicNameValuePair("refresh_token", oldToken.getRefreshToken()),
            new BasicNameValuePair("grant_type", "refresh_token"),
            new BasicNameValuePair("redirect_uri", url)
        )));
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        return new SocialToken(
            name,
            response.get("access_token").asText(),
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(55),
            response.get("refresh_token").asText()
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

    @Override
    public SocialProfile getProfile(SocialToken token) throws IOException {
        HttpGet request = new HttpGet("https://streamlabs.com/api/v1.0/user?access_token=" + token.getToken());
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode rootNode = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        JsonNode twitchNode = rootNode.get("twitch");
        String externalName = twitchNode.get("name").asText().toLowerCase();
        String externalId = twitchNode.get("id").asText();
        return new SocialProfile(externalId, name, externalName, null, token);
    }
}
