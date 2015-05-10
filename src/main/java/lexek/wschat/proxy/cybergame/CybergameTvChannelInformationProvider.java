package lexek.wschat.proxy.cybergame;

import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import lexek.wschat.proxy.ChannelInformationProvider;
import lexek.wschat.proxy.StreamInfo;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

public class CybergameTvChannelInformationProvider implements ChannelInformationProvider {
    private final HttpClient httpClient;
    private final String channel;

    public CybergameTvChannelInformationProvider(HttpClient httpClient, String channel) {
        this.httpClient = httpClient;
        this.channel = channel;
    }

    @Override
    public Long fetchViewerCount() throws IOException {
        Long result = null;
        HttpGet httpGet = new HttpGet("http://api.cybergame.tv/w/streams2.php?channel=" + channel);

        JsonObject rootObject = httpClient.execute(httpGet, JsonResponseHandler.INSTANCE).getAsJsonObject();
        if (rootObject.get("online").getAsString().equals("1")) {
            result = Longs.tryParse(rootObject.get("viewers").getAsString());
        }
        return result;
    }

    @Override
    public StreamInfo fetchFullInfo() throws IOException {
        throw new UnsupportedOperationException();
    }
}
