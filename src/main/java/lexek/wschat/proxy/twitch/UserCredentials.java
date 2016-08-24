package lexek.wschat.proxy.twitch;

class UserCredentials {
    private final String id;
    private final String token;

    UserCredentials(String id, String token) {
        this.id = id;
        this.token = token;
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }
}
