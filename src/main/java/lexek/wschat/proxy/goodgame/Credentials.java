package lexek.wschat.proxy.goodgame;

class Credentials {
    private final String userId;
    private final String token;

    Credentials(String userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }
}
