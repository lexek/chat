package lexek.wschat.proxy.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpMethod;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.util.JsonResponseHandler;
import lexek.wschat.util.OAuthUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TwitterProfileSource {
    private final LoadingCache<String, Long> idCache;
    private final HttpClient httpClient;
    private final TwitterCredentials twitterCredentials;

    public TwitterProfileSource(HttpClient httpClient, TwitterCredentials twitterCredentials) {
        this.httpClient = httpClient;
        this.twitterCredentials = twitterCredentials;
        this.idCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Long>() {
            @Override
            public Long load(String key) throws Exception {
                return getProfileSummary(key).getId();
            }
        });

    }

    public ProfileSummary getProfileSummary(String name) {
        HttpGet httpGet = new HttpGet("https://api.twitter.com/1.1/users/show.json?screen_name=" + name);
        try {
            httpGet.setHeader("Authorization", OAuthUtil.generateAuthorizationHeader(
                twitterCredentials.getConsumerKey(),
                twitterCredentials.getConsumerSecret(),
                twitterCredentials.getAccessToken(),
                twitterCredentials.getAccessTokenSecret(),
                "https://api.twitter.com/1.1/users/show.json",
                HttpMethod.GET,
                ImmutableMap.of("screen_name", name)
            ));
            JsonNode root = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
            long id = root.get("id").asLong();
            idCache.put(name, id);
            return new ProfileSummary(
                name,
                id,
                root.get("protected").asBoolean()
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            throw new InternalErrorException(e);
        }
    }

    public long getTwitterId(String name) {
        return idCache.getUnchecked(name);
    }
}
