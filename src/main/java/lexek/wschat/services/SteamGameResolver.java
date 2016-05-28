package lexek.wschat.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lexek.wschat.chat.e.BadRequestException;
import lexek.wschat.db.dao.SteamGameDao;
import lexek.wschat.db.jooq.tables.pojos.SteamGame;
import lexek.wschat.util.JsonResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SteamGameResolver {
    private final Logger logger = LoggerFactory.getLogger(SteamGameResolver.class);
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private final Cache<Long, String> cache = CacheBuilder.newBuilder()
        .maximumSize(50)
        .build();
    private final SteamGameDao steamGameDao;
    private final HttpClient httpClient;

    public SteamGameResolver(SteamGameDao steamGameDao, HttpClient httpClient) {
        this.steamGameDao = steamGameDao;
        this.httpClient = httpClient;
    }

    public String getName(long id) {
        String cached = cache.getIfPresent(id);
        if (cached == null) {
            cached = steamGameDao.get(id);
            if (cached != null) {
                cache.put(id, cached);
            }
        }
        return cached;
    }

    public void syncDatabase() {
        if (syncInProgress.get()) {
            throw new BadRequestException("sync is already in progress");
        }
        syncInProgress.set(true);
        try {
            JsonNode apps = httpClient.execute(
                new HttpGet("http://api.steampowered.com/ISteamApps/GetAppList/v0002/"),
                JsonResponseHandler.INSTANCE
            ).get("applist").get("apps");
            List<SteamGame> games = new ArrayList<>();
            for (JsonNode node : apps) {
                games.add(new SteamGame(
                    node.get("appid").asLong(),
                    node.get("name").asText()
                ));
            }
            steamGameDao.add(games);
        } catch (IOException e) {
            logger.warn("exception", e);
        } finally {
            syncInProgress.set(false);
            cache.invalidateAll();
        }
    }
}
