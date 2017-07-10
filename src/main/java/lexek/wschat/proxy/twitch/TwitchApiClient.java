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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    public List<Cheermote> getCheermoteCodes() throws URISyntaxException, IOException {
        URI url = new URIBuilder("https://api.twitch.tv/kraken/bits/actions")
            .addParameter("client_id", clientId)
            .build();

        HttpGet request = new HttpGet(url);

        request.addHeader(HttpHeaders.ACCEPT, "application/vnd.twitchtv.v5+json");

        JsonNode rootNode = httpClient.execute(request, JsonResponseHandler.INSTANCE);

        List<Cheermote> result = new ArrayList<>();
        for (JsonNode actionNode : rootNode.get("actions")) {
            String prefix = actionNode.get("prefix").asText();

            String useBg = null;
            for (JsonNode stateNode : actionNode.get("backgrounds")) {
                useBg = stateNode.asText();
                if (useBg.equals("light")) {
                    break;
                }
            }

            String useState = null;
            for (JsonNode stateNode : actionNode.get("states")) {
                useState = stateNode.asText();
                if (useState.equals("animated")) {
                    break;
                }
            }

            List<CheermoteTier> tiers = new ArrayList<>();
            for (JsonNode tierNode : actionNode.get("tiers")) {
                tiers.add(new CheermoteTier(
                    tierNode.get("color").asText(),
                    tierNode.get("images").get(useBg).get(useState).get("1").asText(),
                    tierNode.get("min_bits").asLong()
                ));
            }
            tiers.sort(Comparator.comparingLong(CheermoteTier::getMinBits));

            result.add(new Cheermote(prefix, tiers));
        }
        return result;
    }
}
