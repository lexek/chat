package lexek.wschat.chat.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageNode {
    private final Type type;
    private final String text;
    private long bits;
    private String color;
    private List<MessageNode> children;
    private Style style;
    private String src;
    private Integer width;
    private Integer height;
    @JsonIgnore
    private Long emoticonId;

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

    public static MessageNode emoticonNode(String text, String src, Long id) {
        return MessageNode
            .builder()
            .type(Type.EMOTICON)
            .text(text)
            .src(src)
            .emoticonId(id)
            .build();
    }

    public static MessageNode cheermoteNode(String text, String src, String color, long bits) {
        return MessageNode
            .builder()
            .type(Type.CHEERMOTE)
            .text(text)
            .src(src)
            .color(color)
            .bits(bits)
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
        CHEERMOTE,
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
