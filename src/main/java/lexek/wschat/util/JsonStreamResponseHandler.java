package lexek.wschat.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public enum JsonStreamResponseHandler implements ResponseHandler<JsonParser> {
    INSTANCE;

    private final Logger logger = LoggerFactory.getLogger(JsonStreamResponseHandler.class);
    private final JsonFactory jsonFactory = new JsonFactory();

    @Override
    public JsonParser handleResponse(HttpResponse httpResponse) throws IOException {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        HttpEntity entity = httpResponse.getEntity();
        if (statusCode == 200) {
            if (entity == null) {
                throw new ClientProtocolException("Response contains no content");
            }
            ContentType contentType = ContentType.get(entity);
            Charset charset = contentType.getCharset();
            if (charset == null) {
                charset = StandardCharsets.UTF_8;
            }
            InputStreamReader inputStreamReader = new InputStreamReader(entity.getContent(), charset);
            return jsonFactory.createParser(inputStreamReader);
        } else {
            if (entity != null) {
                logger.debug("bad response code {} {}", statusCode,
                    CharStreams.toString(new InputStreamReader(httpResponse.getEntity().getContent())));
            }
            throw new ClientProtocolException("got status code " + statusCode);
        }
    }
}
