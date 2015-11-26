package lexek.wschat.services;

import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.chat.e.LimitExceededException;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.dao.IgnoreDao;

import java.util.List;

public class IgnoreService {
    private static final int MAX_IGNORED = 20;
    private final IgnoreDao ignoreDao;

    public IgnoreService(IgnoreDao ignoreDao) {
        this.ignoreDao = ignoreDao;
    }

    public List<String> getIgnoredNames(User user) {
        if (user == null) {
            throw new NullPointerException("user");
        }
        return ignoreDao.fetchIgnoreList(user.getWrappedObject());
    }

    public synchronized void ignore(User user, String name) {
        if (user == null) {
            throw new NullPointerException("user");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            throw new InvalidInputException("name", "EMPTY_NAME");
        }
        if (ignoreDao.fetchIgnoreCount(user.getWrappedObject()) >= MAX_IGNORED) {
            throw new LimitExceededException("ignored");
        }
        ignoreDao.addIgnore(user.getWrappedObject(), trimmedName);
    }

    public synchronized void unignore(User user, String name) {
        if (user == null) {
            throw new NullPointerException("user");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            throw new InvalidInputException("name", "EMPTY_NAME");
        }
        if (trimmedName.length() > 16) {
            throw new InvalidInputException("name", "NAME_TOO_LONG");
        }
        ignoreDao.deleteIgnore(user.getWrappedObject(), trimmedName);
    }
}
