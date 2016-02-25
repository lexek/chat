package lexek.wschat.proxy.twitter.entity;

public class MediaEntity extends TweetEntity {
    private final String url;
    private final String displayUrl;

    public MediaEntity(int start, int end, String url, String displayUrl) {
        super(start, end);
        this.url = url;
        this.displayUrl = displayUrl;
    }

    @Override
    public void render(StringBuilder stringBuilder) {
        stringBuilder
            .append("<a href='").append(url).append("' target='_blank'><i class=\"fa fa-fw fa-camera link-icon\"></i>")
            .append(displayUrl).append("</a>");
    }
}
