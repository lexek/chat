package lexek.wschat.proxy.goodgame;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import lexek.wschat.proxy.ProxyAuthService;
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
        List<ProxyEmoticonDescriptor> results = new ArrayList<>();

        HttpGet request = new HttpGet("http://goodgame.ru/api/getchatsmiles2");
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);

        for (JsonNode emoticonNode : response) {
            results.add(new ProxyEmoticonDescriptor(
                ':' + emoticonNode.get("key").asText() + ':',
                emoticonNode.get("images").get("big").asText().replace("https", "http"), //change to http bc cert issue
                emoticonNode.get("key").asText() + ".png",
                ImmutableMap.of(
                    "premium", emoticonNode.get("premium").asBoolean(),
                    "donat", emoticonNode.get("donat").asBoolean(),
                    "channel", emoticonNode.get("channel").asText()
                )
            ));
        }

        return results;
    }
}
