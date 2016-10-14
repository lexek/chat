package lexek.wschat.proxy.goodgame;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;

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
}
