package lexek.wschat.proxy.twitter.entity;

public class SymbolEntity extends TweetEntity {
    private final String symbol;

    public SymbolEntity(int start, int end, String symbol) {
        super(start, end);
        this.symbol = symbol;
    }

    @Override
    public void render(StringBuilder stringBuilder) {
        stringBuilder
            .append("<a href='https://twitter.com/search?q=%24").append(symbol).append("&src=ctag' target='_blank'>")
            .append("<span style='color:#66B5D2'>$</span><strong>")
            .append(symbol)
            .append("</strong></a>");
    }
}
