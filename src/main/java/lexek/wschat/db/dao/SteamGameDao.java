package lexek.wschat.db.dao;

import lexek.wschat.chat.e.InternalErrorException;
import lexek.wschat.db.jooq.tables.pojos.SteamGame;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static lexek.wschat.db.jooq.tables.SteamGame.STEAM_GAME;

@Service
public class SteamGameDao {
    private final DataSource dataSource;

    @Inject
    public SteamGameDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void add(List<SteamGame> steamGames) {
        try (Connection connection = dataSource.getConnection()) {
            DSL.using(connection).transaction(txCtx -> {
                DSLContext create = DSL.using(txCtx);
                create.batch(
                    steamGames
                        .stream()
                        .map(game -> create
                            .insertInto(STEAM_GAME, STEAM_GAME.ID, STEAM_GAME.NAME)
                            .values(game.getId(), game.getName())
                            .onDuplicateKeyIgnore()
                        ).collect(Collectors.toList())
                ).execute();
            });
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
    }

    public String get(long id) {
        String result = null;
        try (Connection connection = dataSource.getConnection()) {
            Record1<String> record = DSL.using(connection)
                .select(STEAM_GAME.NAME)
                .from(STEAM_GAME)
                .where(STEAM_GAME.ID.equal(id))
                .fetchOne();
            if (record != null) {
                result = record.value1();
            }
        } catch (DataAccessException | SQLException e) {
            throw new InternalErrorException(e);
        }
        return result;
    }
}
