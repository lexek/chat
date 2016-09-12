package lexek.wschat.proxy.cybergame;

import com.fasterxml.jackson.databind.JsonNode;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;

@Service
public class CybergameTvApiClient {
    private final HttpClient httpClient;

    @Inject
    public CybergameTvApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getChannelId(String channelName) throws IOException {
        HttpGet httpGet = new HttpGet("http://api.cybergame.tv/p/statusv2/?channel=" + channelName);
        JsonNode rootNode = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
        if (rootNode.isNull()) {
            return null;
        } else {
            return rootNode.get("id").asText();
        }
    }
}
