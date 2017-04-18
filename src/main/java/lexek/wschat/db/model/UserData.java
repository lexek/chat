package lexek.wschat.db.model;

import lexek.wschat.chat.model.User;

import java.util.Map;

public class UserData {
    private final User user;
    private final Map<String, String> authServices;

    public UserData(User user, Map<String, String> authServices) {
        this.user = user;
        this.authServices = authServices;
    }

    public User getUser() {
        return user;
    }

    public Map<String, String> getAuthServices() {
        return authServices;
    }
}
