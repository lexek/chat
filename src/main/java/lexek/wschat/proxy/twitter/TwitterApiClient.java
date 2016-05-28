package lexek.wschat.proxy.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.netty.handler.codec.http.HttpMethod;
import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class TwitterApiClient {
    private final Logger logger = LoggerFactory.getLogger(TwitterApiClient.class);
    private final LoadingCache<String, Long> idCache;
    private final HttpClient httpClient;
    private final TwitterCredentials twitterCredentials;

    public TwitterApiClient(HttpClient httpClient, TwitterCredentials twitterCredentials) {
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
        } catch (IOException e) {
            throw new InternalErrorException(e);
        }
    }

    public long getTwitterId(String name) {
        return idCache.getUnchecked(name);
    }

    public void loadNames(List<String> allNames) {
        for (List<String> names : Lists.partition(allNames, 100)) {
            try {
                String url = "https://api.twitter.com/1.1/users/lookup.json";
                HttpPost httpPost = new HttpPost(url);
                String formattedNames = names.stream().collect(Collectors.joining(","));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(ImmutableList.of(new BasicNameValuePair(
                    "screen_name", formattedNames
                )));
                httpPost.setEntity(entity);
                httpPost.setHeader("Authorization", OAuthUtil.generateAuthorizationHeader(
                    twitterCredentials.getConsumerKey(),
                    twitterCredentials.getConsumerSecret(),
                    twitterCredentials.getAccessToken(),
                    twitterCredentials.getAccessTokenSecret(),
                    url,
                    HttpMethod.POST,
                    ImmutableMap.of("screen_name", formattedNames)
                ));
                JsonNode root = httpClient.execute(httpPost, JsonResponseHandler.INSTANCE);
                for (JsonNode user : root) {
                    idCache.put(user.get("screen_name").asText().toLowerCase(), user.get("id").asLong());
                }
            } catch (Exception e) {
                logger.warn("error", e);
            }
        }
    }
}
