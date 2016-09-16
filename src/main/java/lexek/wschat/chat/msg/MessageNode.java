package lexek.wschat.chat.msg;

import lombok.*;

@Getter @ToString @EqualsAndHashCode @Builder
public class MessageNode {
    private final Type type;
    private final String text;
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
        return MessageNode.builder().type(Type.MENTION).text(name).build();
    }

    public static MessageNode emoticonNode(String text, String src, int width, int height) {
        return MessageNode.builder().type(Type.EMOTICON).text(text).src(src).width(width).height(height).build();
    }

    public enum Type {
        URL,
        EMOTICON,
        EMOJI,
        MENTION,
        STYLED,
        TEXT
    }
}
