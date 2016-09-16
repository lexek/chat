package lexek.wschat.chat.msg;

import com.google.common.collect.Lists;
import lexek.wschat.db.model.Emoticon;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmoticonMessageProcessor implements MessageProcessor {
    private final EmoticonProvider emoticonProvider;

    public EmoticonMessageProcessor(EmoticonProvider emoticonProvider) {
        this.emoticonProvider = emoticonProvider;
    }

    @Override
    public List<MessageNode> process(List<MessageNode> message) {
        LinkedList<MessageNode> messageNodes = Lists.newLinkedList(message);
        for (Emoticon emoticon : emoticonProvider.getEmoticons()) {
            Pattern pattern = emoticon.getPattern();
            for (ListIterator<MessageNode> iterator = messageNodes.listIterator(); iterator.hasNext();) {
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
                        iterator.add(MessageNode.emoticonNode(
                            text.substring(matchStart, matchEnd),
                            emoticon.getFileName(),
                            emoticon.getWidth(),
                            emoticon.getHeight()
                        ));
                    }
                    //add leftovers to result
                    if (start != text.length()) {
                        iterator.add(MessageNode.textNode(text.substring(start, text.length())));
                    }
                }
            }
        }
        return messageNodes;
    }
}
