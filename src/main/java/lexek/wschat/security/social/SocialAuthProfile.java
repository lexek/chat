package lexek.wschat.security.social;

public class SocialAuthProfile {
    private final long id;
    private final String service;
    private final String name;
    private final String token;
    private final String email;

    public SocialAuthProfile(long id, String service, String name, String token, String email) {
        this.id = id;
        this.service = service;
        this.name = name;
        this.token = token;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public String getService() {
        return service;
    }
}
