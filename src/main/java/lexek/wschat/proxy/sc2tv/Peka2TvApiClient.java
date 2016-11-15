package lexek.wschat.proxy.sc2tv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;

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
}
