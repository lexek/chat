package lexek.wschat.chat.msg;

import lexek.wschat.db.model.Emoticon;

import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmoticonMessageProcessor implements MessageProcessor {
    private final EmoticonProvider<? extends Emoticon> emoticonProvider;
    private final String emoticonRoot;

    public EmoticonMessageProcessor(EmoticonProvider<? extends Emoticon> emoticonProvider, String emoticonRoot) {
        this.emoticonProvider = emoticonProvider;
        this.emoticonRoot = emoticonRoot;
    }

    @Override
    public void process(List<MessageNode> message) {
        for (Emoticon emoticon : emoticonProvider.getEmoticons()) {
            Pattern pattern = emoticon.getPattern();
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
                        iterator.add(MessageNode.emoticonNode(
                            text.substring(matchStart, matchEnd),
                            emoticonRoot + '/' +emoticon.getFileName()
                        ));
                    }
                    //add leftovers to result
                    if (start != text.length()) {
                        iterator.add(MessageNode.textNode(text.substring(start, text.length())));
                    }
                }
            }
        }
    }

    @Override
    public boolean handlesChildren() {
        return true;
    }
}
