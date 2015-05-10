package lexek.wschat.frontend.http;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.gson.Gson;
import io.netty.util.CharsetUtil;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SteamNameResolver extends SimpleHttpHandler {
    private final Map<String, String> map;

    public SteamNameResolver() {
        Map<String, String> map1;
        Gson gson = new Gson();
        try {
            map1 = gson.fromJson(Files.toString(new File("steam_lib.json"), CharsetUtil.UTF_8), HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
            map1 = ImmutableMap.of();
        }
        map = map1;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        String appId = request.queryParam("appid");
        String result = map.get(appId);
        if (appId != null && result != null) {
            response.stringContent(result);
        } else {
            response.badRequest();
        }
    }
}
