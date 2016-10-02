package lexek.wschat.chat.msg;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EmojiMessageProcessor implements MessageProcessor {
    private static final Pattern pattern = Pattern.compile(
        "(([\uD83C\uDF00-\uD83D\uDDFF]|[\uD83D\uDE00-\uD83D\uDE4F]|[\uD83D\uDE80-\uD83D\uDEFF]|[\u2600-\u26FF]|" +
            "[\u2700-\u27BF])[\\x{1F3FB}-\\x{1F3FF}]?)");

    @Override
    public void process(List<MessageNode> message) {
        for (ListIterator<MessageNode> iterator = message.listIterator(); iterator.hasNext(); ) {
            MessageNode node = iterator.next();
            if (node.getType() == MessageNode.Type.TEXT) {
                iterator.remove();
                String text = node.getText();
                Matcher matcher = pattern.matcher(node.getText());
                int start = 0;
                while (matcher.find()) {
                    int matchStart = matcher.start();
                    int matchEnd = matcher.end();
                    String before = text.substring(start, matchStart);
                    if (before.length() > 0) {
                        iterator.add(MessageNode.textNode(before));
                    }
                    start = matchEnd;
                    String rawEmoji = matcher.group(1);
                    iterator.add(MessageNode.emojiNode(
                        rawEmoji,
                        grabTheRightIcon(rawEmoji)
                    ));
                }
                //add leftovers to result
                if (start != text.length()) {
                    iterator.add(MessageNode.textNode(text.substring(start, text.length())));
                }
            }
        }
    }

    private static String toCodePoint(String unicodeSurrogates, String sep) {
        ArrayList<String> r = new ArrayList<>();
        int c;
        int p = 0;
        int i = 0;
        if (sep == null)
            sep = "-";
        while (i < unicodeSurrogates.length()) {
            c = unicodeSurrogates.charAt(i++);
            if (p != 0) {
                r.add(Integer.toString((0x10000 + ((p - 0xD800) << 10) + (c - 0xDC00)), 16));
                p = 0;
            } else if (0xD800 <= c && c <= 0xDBFF) {
                p = c;
            } else {
                r.add(Integer.toString(c, 16));
            }
        }
        return r.stream().collect(Collectors.joining(sep));
    }

    private static String grabTheRightIcon(String rawText) {
        // if variant is present as \uFE0F
        return toCodePoint(
            rawText.indexOf('\u200D') < 0 ?
                rawText.replace("\uFE0F", "") :
                rawText, null);
    }

    @Override
    public boolean handlesChildren() {
        return true;
    }
}
