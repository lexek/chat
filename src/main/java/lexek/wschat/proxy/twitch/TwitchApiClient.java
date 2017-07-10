package lexek.wschat.proxy.twitch;

import com.fasterxml.jackson.databind.JsonNode;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

@Service
public class TwitchApiClient {
    private final HttpClient httpClient;
    private final String clientId;

    @Inject
    public TwitchApiClient(
        HttpClient httpClient,
        @Named("twitch.clientId") String clientId
    ) {
        this.httpClient = httpClient;
        this.clientId = clientId;
    }

    public Set<String> getCheermoteCodes() throws URISyntaxException, IOException {
        URI url = new URIBuilder("https://api.twitch.tv/kraken/bits/actions")
            .addParameter("client_id", clientId)
            .build();

        HttpGet request = new HttpGet(url);

        request.addHeader(HttpHeaders.ACCEPT, "application/vnd.twitchtv.v5+json");

        JsonNode rootNode = httpClient.execute(request, JsonResponseHandler.INSTANCE);

        Set<String> result = new HashSet<>();
        for (JsonNode actionNode : rootNode.get("actions")) {
            result.add(actionNode.get("prefix").asText());
        }
        return result;
    }
}
