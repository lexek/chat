package lexek.wschat.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.dao.UserDao;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserData;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.UserChangeSet;
import lexek.wschat.db.tx.Transactional;
import lexek.wschat.security.AuthenticationManager;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    private final LoadingCache<String, Long> userByNameCache;
    private final LoadingCache<Long, User> userCache;
    private final ConnectionManager connectionManager;
    private final AuthenticationManager authenticationManager;
    private final UserDao userDao;
    private final JournalService journalService;

    @Inject
    public UserService(
        ConnectionManager connectionManager,
        AuthenticationManager authenticationManager,
        UserDao userDao,
        JournalService journalService
    ) {
        this.connectionManager = connectionManager;
        this.authenticationManager = authenticationManager;
        this.userDao = userDao;
        this.journalService = journalService;
        this.userByNameCache = Caffeine
            .<String, Long>newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(userDao::getIdByName);
        this.userCache = Caffeine
            .<String, Long>newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(userDao::getById);
    }

    public UserDto fetchById(long id) {
        return userCache.get(id)
    }

    public UserDto fetchByName(String name) {
        Long id = userByNameCache.get(name);
        if (id != null) {
            return fetchById(id);
        }
        return null;
    }

    public UserDto findByNameOrEmail(String name) {
        return userDao.getByNameOrEmail(name);
    }

    @Transactional
    public void update(User user, User admin, UserChangeSet changeSet) {
        User updatedUser = userDao.update(user.getId(), changeSet);
        if (updatedUser != null) {
            userCache.invalidate(user.getName());
            //close all connection authenticated with that user account
            connectionManager.forEach(connection -> user.getId().equals(connection.getUser().getId()), Connection::close);
            journalService.userUpdated(user, admin, changeSet);
        }
    }

    @Transactional
    public boolean changeName(User user, String newName) {
        if (userDao.tryChangeName(user.getId(), newName, user.hasRole(GlobalRole.ADMIN))) {
            journalService.nameChanged(user, user.getName(), newName);
            userByNameCache.invalidate(user.getName());
            userCache.invalidate(user.getId());
            //close all connection authenticated with that user account
            connectionManager.forEach(connection -> user.getId().equals(connection.getUser().getId()), Connection::close);
            return true;
        }
        return false;
    }

    @Transactional
    public void changePassword(User admin, User user, String password) {
        authenticationManager.setPasswordNoCheck(user, password);
        journalService.userPasswordChanged(admin, user);
    }

    public void delete(User user, User admin) {
        if (userDao.delete(user)) {
            userByNameCache.invalidate(user.getName());
            userCache.invalidate(user.getId());
            //close all connection authenticated with that user account
            connectionManager.forEach(connection -> user.getId().equals(connection.getUser().getId()), Connection::close);
        }
    }

    public User cache(UserDto user) { //todo ???
        User instance = userByNameCache.getIfPresent(user.getName());
        if (instance != null) {
            instance.wrap(user);
        } else {
            instance = new lexek.wschat.chat.model.CachedUser(user, cache);
            userByNameCache.put(user.getName(), instance);
        }
        return instance;
    }

    @Deprecated
    public User getCached(String name) {
        return userByNameCache.getIfPresent(name);
    }

    public DataPage<UserData> searchPaged(int page, int pageLength, String search) {
        return userDao.searchPaged(page, pageLength, search);
    }

    public List<User> searchSimple(int pageLength, String search) {
        return userDao.searchSimple(pageLength, search);
    }

    public DataPage<UserData> getAllPaged(Integer page, int pageLength) {
        return userDao.getAllPaged(page, pageLength);
    }

    public List<User> getAdmins() {
        return userDao.getAdmins();
    }

    public boolean checkIfAvailable(String username) {
        return userDao.checkName(username);
    }

    public UserData fetchData(long id) {
        return userDao.fetchData(id);
    }

    public void setCheckIp(User user, boolean value) {
        userDao.setCheckIp(user, value);
    }

    public void setColor(User user, String colorCode) {
        userDao.setColor(user.getId(), colorCode);
        userCache.invalidate(user.getId());
    }
}
