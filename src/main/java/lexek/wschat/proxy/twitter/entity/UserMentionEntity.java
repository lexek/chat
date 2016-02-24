package lexek.wschat.proxy.twitter.entity;

public class UserMentionEntity extends TweetEntity {
    private final String name;

    public UserMentionEntity(int start, int end, String name) {
        super(start, end);
        this.name = name;
    }

    @Override
    public void render(StringBuilder stringBuilder) {
        stringBuilder
            .append("<a href='https://twitter.com/").append(name).append("' target='_blank'>@")
            .append(name).append("</a>");
    }
}
