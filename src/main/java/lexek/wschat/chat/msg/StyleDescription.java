package lexek.wschat.chat.msg;

import java.util.regex.Pattern;

public class StyleDescription {
    private final Pattern pattern;
    private final MessageNode.Style style;

    public StyleDescription(Pattern pattern, MessageNode.Style style) {
        this.pattern = pattern;
        this.style = style;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public MessageNode.Style getStyle() {
        return style;
    }
}
