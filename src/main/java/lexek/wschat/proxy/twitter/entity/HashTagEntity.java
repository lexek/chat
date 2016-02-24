package lexek.wschat.proxy.twitter.entity;

public class HashTagEntity extends TweetEntity {
    private final String hashTag;

    public HashTagEntity(int start, int end, String hashTag) {
        super(start, end);
        this.hashTag = hashTag;
    }

    @Override
    public void render(StringBuilder stringBuilder) {
        stringBuilder
            .append("<a href='https://twitter.com/hashtag/").append(hashTag).append("' target='_blank'>#")
            .append(hashTag).append("</a>");
    }
}
