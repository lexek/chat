package lexek.wschat.proxy.vk;

import com.fasterxml.jackson.databind.JsonNode;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;

import static lexek.wschat.proxy.vk.VkProxyProvider.API_VERSION;

@Service
public class VkApiClient {
    private final HttpClient httpClient;

    @Inject
    public VkApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public JsonNode findLiveVideo(String owner, String token) throws URISyntaxException, IOException {
        int offset = 0;
        int count = 1;
        boolean done = false;
        while (!done) {
            URIBuilder uriBuilder = new URIBuilder("https://api.vk.com/method/video.get");
            uriBuilder.addParameter("access_token", token);
            uriBuilder.addParameter("owner_id", owner);
            uriBuilder.addParameter("count", String.valueOf(count));
            uriBuilder.addParameter("offset", String.valueOf(offset));
            uriBuilder.addParameter("v", API_VERSION);
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            JsonNode rootNode = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);

            if (rootNode.has("error")) {
                throw new RuntimeException("error occured: " + rootNode.get("error"));
            }

            JsonNode items = rootNode.get("response").get("items");
            if (items.size() == 0) {
                done = true;
            }

            for (JsonNode item : items) {
                if (item.has("live") && item.get("live").intValue() == 1) {
                    return item;
                }
            }

            offset += count;
        }

        return null;
    }

    public JsonNode getVideoInfo(String videoId, String token) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder("https://api.vk.com/method/video.get");
        uriBuilder.addParameter("access_token", token);
        uriBuilder.addParameter("videos", videoId);
        uriBuilder.addParameter("count", "1");
        uriBuilder.addParameter("v", API_VERSION);
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        JsonNode rootNode = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);

        if (rootNode.has("error")) {
            throw new RuntimeException("error occured: " + rootNode.get("error"));
        }

        JsonNode items = rootNode.get("response").get("items");
        return items.size() > 0 ? items.get(0) : null;
    }
}
