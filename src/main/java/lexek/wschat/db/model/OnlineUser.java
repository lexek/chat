package lexek.wschat.db.model;

public class OnlineUser {
    private final String ip;
    private final String geoIp;
    private final UserDto user;

    public OnlineUser(String ip, String geoIp, UserDto user) {
        this.ip = ip;
        this.geoIp = geoIp;
        this.user = user;
    }

    public String getIp() {
        return ip;
    }

    public String getGeoIp() {
        return geoIp;
    }

    public UserDto getUser() {
        return user;
    }
}
