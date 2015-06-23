package lexek.wschat.util;

import com.google.common.io.CharStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;

public enum JsonResponseHandler implements ResponseHandler<JsonElement> {
    INSTANCE;

    private final Logger logger = LoggerFactory.getLogger(JsonResponseHandler.class);
    private final JsonParser parser = new JsonParser();

    @Override
    public JsonElement handleResponse(HttpResponse httpResponse) throws IOException {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        HttpEntity entity = httpResponse.getEntity();
        if (statusCode == 200) {
            if (entity == null) {
                throw new ClientProtocolException("Response contains no content");
            }
            InputStreamReader inputStreamReader = new InputStreamReader(entity.getContent());
            return parser.parse(inputStreamReader);
        } else {
            if (entity != null) {
                logger.debug("bad response {} {}", statusCode,
                    CharStreams.toString(new InputStreamReader(httpResponse.getEntity().getContent())));
            }
            throw new ClientProtocolException("got status code " + statusCode);
        }
    }
}
