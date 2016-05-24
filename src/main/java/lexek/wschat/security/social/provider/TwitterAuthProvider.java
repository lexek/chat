package lexek.wschat.security.social.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import lexek.wschat.proxy.twitter.OAuthUtil;
import lexek.wschat.security.social.SocialProfile;
import lexek.wschat.security.social.SocialRedirect;
import lexek.wschat.security.social.SocialToken;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TwitterAuthProvider implements SocialAuthProvider {
    //todo: change to cache with time expiration
    private final boolean signIn;
    private final boolean checkEmail;
    private final Map<String, String> secrets = new HashMap<>();
    private final String name;
    private final String consumerKey;
    private final String consumerSecret;
    private final String url;
    private final HttpClient httpClient;

    public TwitterAuthProvider(
        boolean signIn,
        boolean checkEmail,
        String consumerKey,
        String consumerSecret,
        String url,
        String name,
        HttpClient httpClient
    ) {
        this.signIn = signIn;
        this.checkEmail = checkEmail;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.url = url;
        this.name = name;
        this.httpClient = httpClient;
    }

    private synchronized void addSecret(String token, String secret) {
        secrets.put(token, secret);
    }

    private synchronized String deleteSecret(String token) {
        return secrets.remove(token);
    }

    @Override
    public SocialRedirect getRedirect() throws IOException {
        HttpPost request = new HttpPost("https://api.twitter.com/oauth/request_token");
        request.setHeader(HttpHeaders.AUTHORIZATION, OAuthUtil.generateRequestHeader(
            consumerKey,
            consumerSecret,
            url,
            "https://api.twitter.com/oauth/request_token",
            HttpMethod.POST,
            ImmutableMap.<String, String>of()
        ));
        String response = httpClient.execute(request, new BasicResponseHandler());
        List<NameValuePair> parsedEntities = URLEncodedUtils.parse(response, StandardCharsets.UTF_8);
        Map<String, List<NameValuePair>> map = parsedEntities
            .stream()
            .collect(Collectors.groupingBy(NameValuePair::getName));
        String url = "https://api.twitter.com/oauth/authorize";
        if (signIn) {
            url = "https://api.twitter.com/oauth/authenticate";
        }
        String token = map.get("oauth_token").get(0).getValue();
        String secret = map.get("oauth_token_secret").get(0).getValue();
        addSecret(token, secret);
        return new SocialRedirect(url + "?oauth_token=" + token, token);
    }

    @Override
    public SocialToken authenticate(String token, String verifier) throws IOException {
        HttpPost request = new HttpPost("https://api.twitter.com/oauth/access_token");
        request.setHeader(HttpHeaders.AUTHORIZATION, OAuthUtil.generateAuthorizationHeader(
            consumerKey,
            consumerSecret,
            token,
            deleteSecret(token),
            "https://api.twitter.com/oauth/request_token",
            HttpMethod.POST,
            ImmutableMap.of("oauth_verifier", verifier)
        ));
        request.setEntity(new UrlEncodedFormEntity(ImmutableList.of(
            new BasicNameValuePair("oauth_verifier", verifier)
        )));
        String response = httpClient.execute(request, new BasicResponseHandler());
        Map<String, List<NameValuePair>> map = URLEncodedUtils
            .parse(response, StandardCharsets.UTF_8)
            .stream()
            .collect(Collectors.groupingBy(NameValuePair::getName));
        return new SocialToken(
            this.name,
            map.get("oauth_token").get(0).getValue() + ":" + map.get("oauth_token_secret").get(0).getValue(),
            null,
            null
        );
    }

    @Override
    public SocialToken authenticate(String code) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocialProfile getProfile(SocialToken socialToken) throws IOException {
        Token token = new Token(socialToken.getToken());
        HttpGet request = new HttpGet("https://api.twitter.com/1.1/account/verify_credentials.json" +
            "?skip_status=true" +
            "&include_email=true"
        );
        request.setHeader(HttpHeaders.AUTHORIZATION, OAuthUtil.generateAuthorizationHeader(
            consumerKey,
            consumerSecret,
            token.getKey(),
            token.getSecret(),
            "https://api.twitter.com/1.1/account/verify_credentials.json",
            HttpMethod.GET,
            ImmutableMap.of(
                "skip_status", "true",
                "include_email", "true"
            )
        ));
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        String email = null;
        if (response.get("email") != null) {
            email = response.get("email").asText();
        }
        return new SocialProfile(
            response.get("id_str").asText(),
            this.name,
            response.get("screen_name").asText().toLowerCase(),
            email,
            socialToken
        );
    }

    @Override
    public SocialToken refresh(SocialToken oldToken) throws IOException {
        return oldToken;
    }

    @Override
    public boolean needsRefreshing() {
        return false;
    }

    @Override
    public boolean checkEmail() {
        return checkEmail;
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
    public boolean isV1() {
        return true;
    }

    private class Token {
        private final String secret;
        private final String key;

        private Token(String pair) {
            String[] values = pair.split(":");
            this.key = values[0];
            this.secret = values[1];
        }

        public String getSecret() {
            return secret;
        }

        public String getKey() {
            return key;
        }
    }
}
