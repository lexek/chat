package lexek.wschat.chat.msg;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionMessageProcessor implements MessageProcessor {
    private final Pattern pattern = Pattern.compile("@([a-z][a-z0-9_]{2,16})\\b");

    @Override
    public List<MessageNode> process(List<MessageNode> message) {
        ImmutableList.Builder<MessageNode> result = ImmutableList.builder();
        for (MessageNode node : message) {
            if (node.getType() == MessageNode.Type.TEXT) {
                String text = node.getText();
                Matcher matcher = pattern.matcher(node.getText());
                int start = 0;
                while (matcher.find()) {
                    int matchStart = matcher.start();
                    int matchEnd = matcher.end();
                    String before = text.substring(start, matchStart);
                    if (before.length() > 0) {
                        result.add(MessageNode.textNode(before));
                    }
                    start = matchEnd;
                    result.add(MessageNode.mentionNode(matcher.group(1)));
                }
                //add leftovers to result
                if (start != text.length()) {
                    result.add(MessageNode.textNode(text.substring(start, text.length())));
                }
            } else {
                result.add(node);
            }
        }
        return result.build();
    }
}
