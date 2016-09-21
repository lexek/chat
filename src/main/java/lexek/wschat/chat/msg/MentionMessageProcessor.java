package lexek.wschat.chat.msg;

import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionMessageProcessor implements MessageProcessor {
    private final Pattern pattern = Pattern.compile("@([a-z][a-z0-9_]{2,16})\\b");

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
                    iterator.add(MessageNode.mentionNode(matcher.group(1)));
                }
                //add leftovers to result
                if (start != text.length()) {
                    iterator.add(MessageNode.textNode(text.substring(start, text.length())));
                }
            }
        }
    }
}
