package lexek.wschat.proxy.beam;

import com.fasterxml.jackson.databind.JsonNode;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;

@Service
public class BeamDataProvider {
    private final HttpClient httpClient;

    @Inject
    public BeamDataProvider(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public long getId(String channelName) throws IOException {
        HttpGet httpGet = new HttpGet("https://beam.pro/api/v1/channels/lexek?fields=id");
        JsonNode result = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
        return result.get("id").asLong();
    }

    public String getChatServer(long channelId) throws IOException {
        HttpGet httpGet = new HttpGet("https://beam.pro/api/v1/chats/" + channelId);
        JsonNode result = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
        return result.get("endpoints").get(0).asText();
    }
}
