package lexek.wschat.proxy.twitch;

import com.fasterxml.jackson.databind.JsonNode;
import lexek.wschat.proxy.ChannelInformationProvider;
import lexek.wschat.proxy.StreamInfo;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.time.Instant;

public class TwitchTvChannelInformationProvider implements ChannelInformationProvider {
    private final String channel;
    private final HttpClient httpClient;

    public TwitchTvChannelInformationProvider(String channel, HttpClient httpClient) {
        this.channel = channel;
        this.httpClient = httpClient;
    }

    @Override
    public Long fetchViewerCount() throws IOException {
        return fetchFullInfo().getViewers();
    }

    @Override
    public StreamInfo fetchFullInfo() throws IOException {
        StreamInfo result = null;
        HttpGet httpGet = new HttpGet("https://api.twitch.tv/kraken/streams/" + channel);

        JsonNode rootObject = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
        JsonNode streamObject = rootObject.get("stream");
        if (streamObject != null && !streamObject.isNull()) {
            long viewers = streamObject.get("viewers").asLong();
            long started = Instant.parse(streamObject.get("created_at").asText()).toEpochMilli();
            long id = streamObject.get("_id").asLong();
            String title = streamObject.get("channel").get("status").asText();
            result = new StreamInfo(id, started, title, viewers);
        }
        return result;
    }
}
