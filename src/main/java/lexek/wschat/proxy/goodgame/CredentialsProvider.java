package lexek.wschat.proxy.goodgame;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import lexek.wschat.proxy.ProxyAuthService;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

class CredentialsProvider {
    private final HttpClient httpClient;
    private final ProxyAuthService proxyAuthService;
    private final long authId;

    public CredentialsProvider(HttpClient httpClient, ProxyAuthService proxyAuthService, long authId) {
        this.httpClient = httpClient;
        this.proxyAuthService = proxyAuthService;
        this.authId = authId;
    }

    public Credentials getCredentials() throws IOException {
        String token = proxyAuthService.getToken(authId);
        HttpGet request = new HttpGet("http://api2.goodgame.ru/chat/token");
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        JsonNode response = httpClient.execute(request, JsonResponseHandler.INSTANCE);
        String chatToken = response.get("chat_token").asText();
        String userId = response.get("user_id").asText();
        return new Credentials(userId, chatToken);
    }
}
