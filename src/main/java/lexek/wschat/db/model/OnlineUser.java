package lexek.wschat.db.model;

public class OnlineUser {
    private final String ip;
    private final UserDto user;

    public OnlineUser(String ip, UserDto user) {
        this.ip = ip;
        this.user = user;
    }

    public String getIp() {
        return ip;
    }

    public UserDto getUser() {
        return user;
    }
}
