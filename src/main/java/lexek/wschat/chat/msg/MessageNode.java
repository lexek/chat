package lexek.wschat.chat.msg;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.LinkedList;
import java.util.List;

@Getter @ToString @EqualsAndHashCode @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageNode {
    private final Type type;
    private final String text;
    private List<MessageNode> children;
    private Style style;
    private String src;
    private Integer width;
    private Integer height;

    public static MessageNode textNode(String text) {
        return MessageNode.builder().type(Type.TEXT).text(text).build();
    }

    public static MessageNode urlNode(String url) {
        return MessageNode.builder().type(Type.URL).text(url).build();
    }

    public static MessageNode mentionNode(String name) {
        return MessageNode
            .builder()
            .type(Type.MENTION)
            .text(name)
            .build();
    }

    public static MessageNode emoticonNode(String text, String src, int width, int height) {
        return MessageNode
            .builder()
            .type(Type.EMOTICON)
            .text(text)
            .src(src)
            .width(width)
            .height(height)
            .build();
    }

    public static MessageNode emojiNode(String text, String src) {
        return MessageNode
            .builder()
            .type(Type.EMOJI)
            .text(text)
            .src(src)
            .build();
    }

    public static MessageNode styledNode(String text, List<MessageNode> children, Style style) {
        return MessageNode
            .builder()
            .type(Type.STYLED)
            .text(text)
            .children(children)
            .style(style)
            .build();
    }

    public enum Type {
        URL,
        EMOTICON,
        EMOJI,
        MENTION,
        STYLED,
        TEXT
    }

    public enum Style {
        BOLD,
        SPOILER,
        ITALIC,
        NSFW,
        QUOTE,
        STRIKETHROUGH
    }
}
