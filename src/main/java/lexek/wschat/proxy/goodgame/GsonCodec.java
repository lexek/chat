package lexek.wschat.proxy.goodgame;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ChannelHandler.Sharable
public class GsonCodec extends MessageToMessageCodec<TextWebSocketFrame, JsonElement> {
    Logger logger = LoggerFactory.getLogger(GsonCodec.class);
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    @Override
    protected void encode(ChannelHandlerContext ctx, JsonElement msg, List<Object> out) throws Exception {
        out.add(new TextWebSocketFrame(gson.toJson(msg)));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame msg, List<Object> out) throws Exception {
        logger.trace("raw message: {}", msg);
        out.add(jsonParser.parse(msg.text()));
    }
}
