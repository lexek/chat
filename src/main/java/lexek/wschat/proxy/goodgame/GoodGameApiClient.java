package lexek.wschat.proxy.goodgame;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.proxy.ProxyEmoticonDescriptor;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
class GoodGameApiClient {
    private final HttpClient httpClient;
    private final ProxyAuthService proxyAuthService;

    @Inject
    public GoodGameApiClient(HttpClient httpClient, ProxyAuthService proxyAuthService) {
        this.httpClient = httpClient;
        this.proxyAuthService = proxyAuthService;
    }

    public Credentials getCredentials(long authId) throws IOException {
        String token = proxyAuthService.getToken(authId);
        HttpGet request = new HttpGet("http://api2.goodgame.ru/chat/token");
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        String chatToken = response.get("chat_token").asText();
        String userId = response.get("user_id").asText();
        return new Credentials(userId, chatToken);
    }

    public Long getChannelId(String channelName) throws IOException {
        HttpGet request = new HttpGet("http://api2.goodgame.ru/streams/" + channelName);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        return response.get("channel").get("id").asLong();
    }

    public List<ProxyEmoticonDescriptor> getEmoticons() throws Exception {
        int page = 0;
        int totalPages = 1;
        List<ProxyEmoticonDescriptor> results = new ArrayList<>();
        while (page < totalPages) {
            page++;
            URIBuilder uriBuilder = new URIBuilder("http://api2.goodgame.ru/smiles");
            uriBuilder.addParameter("page", String.valueOf(page));
            HttpGet request = new HttpGet(uriBuilder.build());
            request.setHeader(HttpHeaders.ACCEPT, "application/json");
            JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);
            totalPages = response.get("page_count").asInt();
            for (JsonNode emoticonNode : response.get("_embedded").get("smiles")) {
                results.add(new ProxyEmoticonDescriptor(
                    ':' + emoticonNode.get("key").asText() + ':',
                    emoticonNode.get("urls").get("img").asText(),
                    emoticonNode.get("key").asText() + ".png",
                    ImmutableMap.of(
                        "donate_lvl", emoticonNode.get("donate_lvl").asLong(),
                        "is_premium", emoticonNode.get("is_premium").asBoolean(),
                        "is_paid", emoticonNode.get("is_paid").asBoolean(),
                        "channel_id", emoticonNode.get("channel_id").asText()
                    )
                ));
            }
        }
        return results;
    }
}
