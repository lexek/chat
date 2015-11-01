package lexek.wschat.services;

import com.google.common.collect.ImmutableMap;
import lexek.wschat.chat.User;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.db.dao.IgnoreDao;

import java.util.List;

public class IgnoreService {
    private final IgnoreDao ignoreDao;

    public IgnoreService(IgnoreDao ignoreDao) {
        this.ignoreDao = ignoreDao;
    }

    public List<String> getIgnoredNames(User user) {
        if (user == null) {
            throw new NullPointerException("user");
        }
        return ignoreDao.getIgnoreList(user.getWrappedObject());
    }

    public void ignore(User user, String name) {
        if (user == null) {
            throw new NullPointerException("user");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            throw new InvalidInputException(ImmutableMap.of("name", "EMPTY_NAME"));
        }
        ignoreDao.addIgnore(user.getWrappedObject(), trimmedName);
    }

    public void unignore(User user, String name) {
        if (user == null) {
            throw new NullPointerException("user");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            throw new InvalidInputException(ImmutableMap.of("name", "EMPTY_NAME"));
        }
        ignoreDao.deleteIgnore(user.getWrappedObject(), trimmedName);
    }
}
