package lexek.wschat.proxy.cybergame;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import lexek.wschat.proxy.ProxyEmoticonDescriptor;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CybergameTvApiClient {
    private final HttpClient httpClient;

    @Inject
    public CybergameTvApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getChannelId(String channelName) throws IOException {
        HttpGet httpGet = new HttpGet("https://api.cybergame.tv/p/statusv2/?channel=" + channelName);
        JsonNode rootNode = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
        if (rootNode.isNull()) {
            return null;
        } else {
            return rootNode.get("id").asText();
        }
    }

    public List<ProxyEmoticonDescriptor> getEmoticons() throws Exception {
        //todo: investigate on emotion api & path
        HttpGet httpGet = new HttpGet("https://cybergame.tv/chats/beta1210162/emotes/emotes.json");
        JsonNode rootNode = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
        List<ProxyEmoticonDescriptor> result = new ArrayList<>();
        processNodes(rootNode.get("global"), result, "");
        processNodes(rootNode.get("premium"), result, "");
        for (JsonNode channelNode : rootNode.get("channel")) {
            String channelId = channelNode.get("id").asText();
            processNodes(channelNode.get("emotes"), result, channelId);
        }
        return result;
    }

    private void processNodes(JsonNode array, List<ProxyEmoticonDescriptor> result, String channelId) {
        for (JsonNode node : array) {
            String widthString = node.get("width:").asText();
            String heightString = node.get("height").asText();

            result.add(new ProxyEmoticonDescriptor(
                node.get("text").asText(),
                "https://cybergame.tv/chats/beta1210162/emotes/" + node.get("image").asText(),
                node.get("image").asText(),
                ImmutableMap.of(
                    "width", Integer.parseInt(widthString.substring(0, widthString.length() - 2)),
                    "height", Integer.parseInt(heightString.substring(0, heightString.length() - 2)),
                    "channel_id", channelId
                )
            ));
        }
    }
}
