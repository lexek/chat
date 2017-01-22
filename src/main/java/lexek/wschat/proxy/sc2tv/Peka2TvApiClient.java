package lexek.wschat.proxy.sc2tv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import lexek.wschat.proxy.ProxyEmoticonDescriptor;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class Peka2TvApiClient {
    private static final String BASE_URL = "https://funstream.tv";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public Peka2TvApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Long getStreamId(String name) throws IOException {
        ObjectNode requestNode = JsonNodeFactory.instance.objectNode();
        requestNode.put("name", name);

        HttpPost request = new HttpPost(BASE_URL + "/api/user");
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(requestNode), ContentType.APPLICATION_JSON));
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);

        return response.get("id").asLong();
    }

    public List<ProxyEmoticonDescriptor> getEmoticons() throws IOException {
        List<ProxyEmoticonDescriptor> results = new ArrayList<>();

        HttpPost request = new HttpPost(BASE_URL + "/api/smile");
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);

        for (JsonNode emoticonNode : response) {
            ImmutableMap.Builder<String, Object> paramsBuilder = ImmutableMap.builder();
            paramsBuilder
                .put("level", emoticonNode.get("level").asBoolean())
                .put("masterStreamerLevel", emoticonNode.get("masterStreamerLevel").asBoolean())
                .put("siteLevel", emoticonNode.get("siteLevel").asText());

            JsonNode user = emoticonNode.get("user");
            if (user != null && !user.isNull()) {
                paramsBuilder.put("user", user.get("id").asLong());
            }

            results.add(new ProxyEmoticonDescriptor(
                ':' + emoticonNode.get("code").asText() + ':',
                emoticonNode.get("url").asText(),
                emoticonNode.get("id").asText() + ".png",
                paramsBuilder.build()
            ));
        }

        return results;
    }
}
