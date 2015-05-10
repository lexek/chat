package lexek.wschat.proxy.twitch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

        JsonObject rootObject = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE).getAsJsonObject();
        JsonElement streamElement = rootObject.get("stream");
        if (streamElement != null && !streamElement.isJsonNull()) {
            JsonObject streamObject = streamElement.getAsJsonObject();
            long viewers = streamObject.get("viewers").getAsLong();
            long started = Instant.parse(streamObject.get("created_at").getAsString()).toEpochMilli();
            long id = streamObject.get("_id").getAsLong();
            String title = streamObject.get("channel").getAsJsonObject().get("status").getAsString();
            result = new StreamInfo(id, started, title, viewers);
        }
        return result;
    }
}
