package lexek.wschat.proxy.goodgame;

import com.google.common.primitives.Longs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

        JsonElement rootElement = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE);
        if (rootElement.isJsonObject()) {
            JsonObject rootObject = rootElement.getAsJsonObject();
            if (rootObject.has(channel)) {
                JsonObject streamObject = rootObject.getAsJsonObject(channel);
                result = Longs.tryParse(streamObject.get("viewers").getAsString());
            }
        }
        return result;
    }

    @Override
    public StreamInfo fetchFullInfo() throws IOException {
        throw new UnsupportedOperationException();
    }
}
