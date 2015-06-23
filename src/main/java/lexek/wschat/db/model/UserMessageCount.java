package lexek.wschat.db.model;

public class UserMessageCount {
    private final String name;
    private final long userId;
    private final long count;

    public UserMessageCount(String name, long userId, long count) {
        this.name = name;
        this.userId = userId;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public long getUserId() {
        return userId;
    }

    public long getCount() {
        return count;
    }
}
