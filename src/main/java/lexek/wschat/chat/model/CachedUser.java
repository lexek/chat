package lexek.wschat.chat.model;

import com.github.benmanes.caffeine.cache.LoadingCache;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.util.Colors;

public class CachedUser implements User {
    public static final User UNAUTHENTICATED_USER = new UserDto(
        null,
        "unauthenticated",
        GlobalRole.UNAUTHENTICATED,
        Colors.generateColor("unauthenticated"),
        false,
        false,
        null,
        false,
        false
    );

    private final Long id;
    private final LoadingCache<Long, User> cache;

    public CachedUser(Long id, LoadingCache<Long, User> cache) {
        this.id = id;
        this.cache = cache;
    }

    @Override
    public String getColor() {
        return getUser().getColor();
    }

    @Override
    public String getName() {
        return getUser().getName();
    }

    @Override
    public GlobalRole getRole() {
        return getUser().getRole();
    }

    @Override
    public boolean isBanned() {
        return getUser().isBanned();
    }

    @Override
    public boolean isRenameAvailable() {
        return getUser().isRenameAvailable();
    }

    @Override
    public Long getId() {
        return getUser().getId();
    }

    private User getUser() {
        return cache.get(this.id);
    }
}
