package lexek.wschat.db.model;

import java.util.Map;

public class UserData {
    private final UserDto user;
    private final Map<String, String> authServices;

    public UserData(UserDto user, Map<String, String> authServices) {
        this.user = user;
        this.authServices = authServices;
    }

    public UserDto getUser() {
        return user;
    }

    public Map<String, String> getAuthServices() {
        return authServices;
    }
}
