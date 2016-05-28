package lexek.wschat.security.social;

public class SocialProfile {
    private final String id;
    private final String service;
    private final String name;
    private final String email;
    private final SocialToken token;

    public SocialProfile(String id, String service, String name, String email, SocialToken token) {
        this.id = id;
        this.service = service;
        this.name = name;
        this.email = email;
        this.token = token;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getService() {
        return service;
    }

    public SocialToken getToken() {
        return token;
    }
}
