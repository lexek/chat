package lexek.wschat.frontend.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
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
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            map1 = objectMapper.readValue(new File("steam_lib.json"), HashMap.class);
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
