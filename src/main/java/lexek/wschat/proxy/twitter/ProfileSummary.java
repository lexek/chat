package lexek.wschat.proxy.twitter;

public class ProfileSummary {
    private final String name;
    private final long id;
    private final boolean isProtected;

    public ProfileSummary(String name, long id, boolean isProtected) {
        this.name = name;
        this.id = id;
        this.isProtected = isProtected;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public boolean isProtected() {
        return isProtected;
    }
}
