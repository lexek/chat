package lexek.wschat.db.model;

import lexek.wschat.chat.model.User;

public class OnlineUser {
    private final String ip;
    private final String geoIp;
    private final User user;

    public OnlineUser(String ip, String geoIp, User user) {
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

    public User getUser() {
        return user;
    }
}
