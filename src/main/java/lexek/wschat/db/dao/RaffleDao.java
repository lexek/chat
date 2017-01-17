package lexek.wschat.db.dao;

import org.jooq.DSLContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class RaffleDao {
    private final DSLContext ctx;

    @Inject
    public RaffleDao(DSLContext ctx) {
        this.ctx = ctx;
    }
}
