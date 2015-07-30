package lexek.wschat.proxy.goodgame;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.primitives.Longs;
import lexek.wschat.proxy.ChannelInformationProvider;
import lexek.wschat.proxy.StreamInfo;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

public class GoodGameChannelInformationProvider implements ChannelInformationProvider {
    private final HttpClient httpClient;
    private final String channel;

    public GoodGameChannelInformationProvider(HttpClient httpClient, String channel) {
        this.httpClient = httpClient;
        this.channel = channel;
    }

    @Override
    public Long fetchViewerCount() throws IOException {
        Long result = null;
        HttpGet httpGet = new HttpGet("http://goodgame.ru/api/getchannelstatus?fmt=json&id=" + channel);

        JsonNode rootObject = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
        if (rootObject.has(channel)) {
            JsonNode streamObject = rootObject.get(channel);
            result = Longs.tryParse(streamObject.get("viewers").asText());
        }
        return result;
    }

    @Override
    public StreamInfo fetchFullInfo() throws IOException {
        throw new UnsupportedOperationException();
    }
}
