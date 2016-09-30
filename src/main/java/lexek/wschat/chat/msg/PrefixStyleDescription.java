package lexek.wschat.chat.msg;

public class PrefixStyleDescription {
    private final String prefix;
    private final MessageNode.Style style;

    public PrefixStyleDescription(String prefix, MessageNode.Style style) {
        this.prefix = prefix;
        this.style = style;
    }

    public String getPrefix() {
        return prefix;
    }

    public MessageNode.Style getStyle() {
        return style;
    }
}
