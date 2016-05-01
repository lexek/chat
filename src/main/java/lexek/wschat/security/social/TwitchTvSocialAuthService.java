package lexek.wschat.security.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;
import com.google.common.net.UrlEscapers;
import io.netty.util.CharsetUtil;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.security.SecureTokenGenerator;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class TwitchTvSocialAuthService implements SocialAuthService {
    private final String name;
    private final String clientId;
    private final String secret;
    private final String url;
    private final HttpClient httpClient;
    private final SecureTokenGenerator secureTokenGenerator;
    private final String scopesString;

    public TwitchTvSocialAuthService(
        String clientId,
        String secret,
        String url,
        Set<String> scopes,
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
        this.scopesString = scopes.stream().collect(Collectors.joining(" "));
    }

    public TwitchTvSocialAuthService(
        String clientId,
        String secret,
        String url,
        HttpClient httpClient,
        SecureTokenGenerator secureTokenGenerator,
        String name
    ) {
        this(clientId, secret, url, ImmutableSet.of("user_read", "chat_login"), name, httpClient, secureTokenGenerator);
    }

    @Override
    public SocialRedirect getRedirect() {
        String state = secureTokenGenerator.generateRandomToken(32).replace("=", "");
        String result = "https://api.twitch.tv/kraken/oauth2/authorize?response_type=code&client_id=" + clientId +
            "&redirect_uri=" + UrlEscapers.urlPathSegmentEscaper().escape(url) +
            "&scope=" + UrlEscapers.urlPathSegmentEscaper().escape(scopesString) +
            "&state=" + UrlEscapers.urlPathSegmentEscaper().escape(state);
        return new SocialRedirect(result, state);
    }

    @Override
    public SocialToken authenticate(String code) throws IOException {
        HttpPost request = new HttpPost("https://api.twitch.tv/kraken/oauth2/token");
        HttpEntity entity = new UrlEncodedFormEntity(ImmutableList.of(
            new BasicNameValuePair("client_secret", secret),
            new BasicNameValuePair("client_id", clientId),
            new BasicNameValuePair("grant_type", "authorization_code"),
            new BasicNameValuePair("redirect_uri", url),
            new BasicNameValuePair("code", code)
        ), CharsetUtil.UTF_8);
        request.setEntity(entity);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        return new SocialToken(name, response.get("access_token").asText(), null, null);
    }

    @Override
    public SocialToken refresh(SocialToken token) throws IOException {
        return null;
    }

    @Override
    public boolean needsRefreshing() {
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
        HttpGet request = new HttpGet("https://api.twitch.tv/kraken/user?oauth_token=" + token.getToken());
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode userData = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        JsonNode emailElem = userData.get("email");
        String email = null;
        if (!emailElem.isNull()) {
            email = emailElem.asText();
        }
        String externalName = userData.get("name").asText().toLowerCase();
        long externalId = userData.get("_id").asLong();
        return new SocialProfile(String.valueOf(externalId), name, externalName, email, token);
    }

    public Set<String> getScopes(String token) throws IOException {
        HttpGet request = new HttpGet("https://api.twitch.tv/kraken?oauth_token=" + token);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode root = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        JsonNode tokenData = root.get("token");
        boolean isValid = tokenData.get("valid").asBoolean();
        if (!isValid) {
            throw new InvalidInputException("token", "invalid token");
        }
        JsonNode authNode = tokenData.get("authorization");
        return StreamSupport.stream(authNode.get("scopes").spliterator(), false)
            .map(JsonNode::asText)
            .collect(Collectors.toSet());
    }
}
