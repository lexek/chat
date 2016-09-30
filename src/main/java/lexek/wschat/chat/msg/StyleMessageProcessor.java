package lexek.wschat.chat.msg;

import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyleMessageProcessor implements MessageProcessor {
    private final List<StyleDescription> styles;
    private final MessageProcessingService processingService;

    public StyleMessageProcessor(
        List<StyleDescription> styles,
        MessageProcessingService processingService
    ) {
        this.styles = styles;
        this.processingService = processingService;
    }

    @Override
    public void process(List<MessageNode> message) {
        for (StyleDescription styleDescription : styles) {
            Pattern pattern = styleDescription.getPattern();
            MessageNode.Style style = styleDescription.getStyle();
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
                        String nodeText = matcher.group(1);
                        MessageNode newNode = MessageNode.styledNode(
                            nodeText,
                            processingService.processMessage(nodeText, false),
                            style
                        );
                        iterator.add(newNode);
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
    public boolean handlesRoot() {
        return true;
    }

    @Override
    public boolean handlesChildren() {
        return true;
    }
}
