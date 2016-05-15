package lexek.wschat.db.model;

public class UserData {
    private final UserDto user;
    //todo: change to map authservice->name
    private final String authServices;
    private final String authNames;

    public UserData(UserDto user, String authServices, String authNames) {
        this.user = user;
        this.authServices = authServices;
        this.authNames = authNames;
    }

    public UserDto getUser() {
        return user;
    }

    public String getAuthServices() {
        return authServices;
    }

    public String getAuthNames() {
        return authNames;
    }
}
