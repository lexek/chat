package lexek.wschat.db.dao;

import lexek.wschat.db.jooq.tables.pojos.SteamGame;
import lexek.wschat.db.tx.Transactional;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.SteamGame.STEAM_GAME;

@Service
public class SteamGameDao {
    private final DSLContext ctx;

    @Inject
    public SteamGameDao(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Transactional
    public void add(List<SteamGame> steamGames) {
        ctx.batch(
            steamGames
                .stream()
                .map(game -> ctx
                    .insertInto(STEAM_GAME, STEAM_GAME.ID, STEAM_GAME.NAME)
                    .values(game.getId(), game.getName())
                    .onDuplicateKeyIgnore()
                ).collect(Collectors.toList())
        ).execute();
    }

    public String get(long id) {
        Record1<String> record = ctx
            .select(STEAM_GAME.NAME)
            .from(STEAM_GAME)
            .where(STEAM_GAME.ID.equal(id))
            .fetchOne();
        if (record != null) {
            return record.value1();
        }
        return null;
    }
}
