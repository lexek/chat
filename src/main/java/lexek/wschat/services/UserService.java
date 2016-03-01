package lexek.wschat.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.ConnectionManager;
import lexek.wschat.chat.model.GlobalRole;
import lexek.wschat.chat.model.User;
import lexek.wschat.db.dao.UserDao;
import lexek.wschat.db.model.DataPage;
import lexek.wschat.db.model.UserData;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.form.UserChangeSet;
import lexek.wschat.security.AuthenticationManager;

import java.util.List;

public class UserService {
    private final Cache<String, User> userCache = CacheBuilder.newBuilder().weakValues().build();
    private final ConnectionManager connectionManager;
    private final AuthenticationManager authenticationManager;
    private final UserDao userDao;
    private final JournalService journalService;

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
    }

    public UserDto fetchById(long id) {
        return userDao.getById(id);
    }

    public UserDto fetchByName(String name) {
        return userDao.getByName(name);
    }

    public void update(UserDto user, UserDto admin, UserChangeSet changeSet) {
        UserDto updatedUser = userDao.update(user.getId(), changeSet);
        if (updatedUser != null) {
            invalidate(user.getName());
            //close all connection authenticated with that user account
            connectionManager.forEach(connection -> user.getId().equals(connection.getUser().getId()), Connection::close);
            journalService.userUpdated(user, admin, changeSet);
        }
    }

    public boolean changeName(UserDto user, String newName) {
        if (userDao.tryChangeName(user.getId(), newName, user.hasRole(GlobalRole.ADMIN))) {
            journalService.nameChanged(user, user.getName(), newName);
            userCache.invalidate(user.getName());
            //close all connection authenticated with that user account
            connectionManager.forEach(connection -> user.getId().equals(connection.getUser().getId()), Connection::close);
            return true;
        }
        return false;
    }

    public void changePassword(UserDto admin, UserDto user, String password) {
        authenticationManager.setPasswordNoCheck(user, password);
        journalService.userPasswordChanged(admin, user);
    }

    public void delete(UserDto user, UserDto admin) {
        if (userDao.delete(user)) {
            userCache.invalidate(user.getName());
            //close all connection authenticated with that user account
            connectionManager.forEach(connection -> user.getId().equals(connection.getUser().getId()), Connection::close);
        }
    }

    public void invalidate(String name) {
        userCache.invalidate(name);
    }

    public User cache(UserDto user) {
        User instance = userCache.getIfPresent(user.getName());
        if (instance != null) {
            instance.wrap(user);
        } else {
            instance = new User(user);
            userCache.put(user.getName(), instance);
        }
        return instance;
    }

    public User getCached(String name) {
        return userCache.getIfPresent(name);
    }

    public DataPage<UserData> searchPaged(int page, int pageLength, String search) {
        return userDao.searchPaged(page, pageLength, search);
    }

    public DataPage<UserData> getAllPaged(Integer page, int pageLength) {
        return userDao.getAllPaged(page, pageLength);
    }

    public List<UserDto> getAdmins() {
        return userDao.getAdmins();
    }

    public boolean checkIfAvailable(String username) {
        return userDao.checkName(username);
    }

    public UserData fetchData(long id) {
        return userDao.fetchData(id);
    }
}
